<script setup lang="ts">
/**
 * 学生管理（菜单「基础信息 -> 学生管理」，url = /student/index）。
 *
 * 对着老 Thymeleaf 版 templates/student/studentes.html 搬的：查询条件（学号 / 姓名 /
 * 学院 / 年级 / 班级）、表格、新增/修改弹窗、重置密码、学生详情。
 *
 * 关键点：
 * 1. 年级和学院都不存在 student 表里，只有 classId —— 它们是顺着 class 表 join 出来的。
 *    所以表单里「学院 -> 年级 -> 班级」是联动筛选，真正提交的只有 classId。
 * 2. 班级下拉走 /api/class/list（不分页），权限是 baseinfo:class:access ——
 *    学生角色也有这条权限，所以学生本人打开这个页面时下拉框不会空。
 * 3. 数据范围由后端兜底：roleType 为「个人」的学生，列表接口只会返回他自己那一条，
 *    改 URL 参数或直接访问别人的 info 都会被拒（见 ApiStudentController）。
 * 4. 没有删除功能 —— menu 表里「学生管理」本来就没有删除权限。
 */
import { nextTick, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules, UploadRequestOptions } from 'element-plus'
import { Plus, Refresh, Search, Upload } from '@element-plus/icons-vue'
import Pagination from '@/components/Pagination.vue'
import { usePageTable } from '@/composables/usePageTable'
import { useLoginStore } from '@/stores/loginStores'
import { useBaseInfoStore } from '@/stores/baseInfoStore'
import { listClassesApi } from '@/api/classApi'
import {
  getStudentInfo,
  getStudentList,
  resetStudentPassword,
  saveStudent,
  uploadStudentPhoto,
} from '@/api/studentApi'
import { emptyStudentForm, type StudentForm, type StudentVo } from '@/type/studentPojo'
import type { ClassPojo } from '@/type/classPojo'
import { BASE_URL } from '@/utils/config'

const loginStore = useLoginStore()
const baseInfoStore = useBaseInfoStore()

// 权限串必须和后端 ApiStudentController 上的 @RequiresPermissions 完全一致
const PERM_ADD = 'baseinfo:student:add'
const PERM_UPDATE = 'baseinfo:student:update'
const PERM_PWD = 'baseinfo:student:updatepassword'
const PERM_DETAIL = 'baseinfo:student:detail'

const page = usePageTable<
  StudentVo,
  { num: string; name: string; collegeId?: number; grade?: number; classId?: number }
>((params) => getStudentList(params), {
  query: { num: '', name: '', collegeId: undefined, grade: undefined, classId: undefined },
})

/** 头像地址：后端给的是 /studentPhoto/xxx.jpg，静态资源挂在 context-path 下 */
const avatarSrc = (url?: string | null) => (url ? `${BASE_URL}${url}` : '')

const sexLabel = (sex?: number | null) => baseInfoStore.labelOf('sexTypes', sex) || '-'

/** 按学院 + 年级取班级列表，两个条件都可以省略（省略即不筛） */
const loadClasses = async (collegeId?: number, grade?: number): Promise<ClassPojo[]> => {
  try {
    return await listClassesApi({ collegeId, grade })
  } catch {
    return []
  }
}

/* ---------------- 查询栏里的班级联动 ---------------- */

const filterClasses = ref<ClassPojo[]>([])

const refreshFilterClasses = async () => {
  filterClasses.value = await loadClasses(page.form.collegeId, page.form.grade)
}

// 学院或年级一变，之前选中的班级可能已经不在候选范围里，先清掉再重新取。
// 用 debounce 没必要 —— 下拉框一次只变一个值。
watch(
  () => [page.form.collegeId, page.form.grade] as const,
  () => {
    page.form.classId = undefined
    void refreshFilterClasses()
  },
)

onMounted(() => {
  void refreshFilterClasses()
})

/* ---------------- 新增 / 修改弹窗 ---------------- */

const dialogVisible = ref(false)
const dialogTitle = ref('新增学生')
const submitting = ref(false)
const uploading = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<StudentForm>(emptyStudentForm())
const formClasses = ref<ClassPojo[]>([])

/**
 * 弹窗里「学院 / 年级」的选择值。它们**不属于**提交给后端的表单 ——
 * student 表只有 classId，学院和年级是顺着 class 表 join 出来的，
 * 单独放一个对象是为了不把它们混进 StudentForm 的请求体里。
 */
const cascade = reactive<{ collegeId: number | null; grade: number | null }>({
  collegeId: null,
  grade: null,
})

/**
 * 打开弹窗时要把已有的 classId / 学院 / 年级 一次性填进去，但联动 watch 会在
 * 学院变化时清空 classId —— 那样编辑时就会把学生的班级丢掉。这个开关让
 * openAdd / openEdit 期间的联动逻辑暂时失效，由它们自己控制顺序。
 */
let suspendCascade = false

const refreshFormClasses = async () => {
  formClasses.value = await loadClasses(cascade.collegeId ?? undefined, cascade.grade ?? undefined)
}

watch(
  () => [cascade.collegeId, cascade.grade] as const,
  () => {
    if (suspendCascade) return
    form.classId = null
    void refreshFormClasses()
  },
)

const rules = reactive<FormRules>({
  num: [
    { required: true, message: '请输入学号', trigger: 'blur' },
    // student.num 是 varchar(12)，后端 @Size(max = 20) 拦不住 13~20 位的输入，
    // 所以这里按库表实际长度挡一下。
    { max: 12, message: '学号不超过 12 个字符', trigger: 'blur' },
  ],
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { max: 50, message: '姓名不超过 50 个字符', trigger: 'blur' },
  ],
  classId: [{ required: true, message: '请选择班级', trigger: 'change' }],
  sex: [{ required: true, message: '请选择性别', trigger: 'change' }],
  mobile: [
    {
      // 后端 @Pattern 已放开空串（表示没填），这里保持同样的规则
      validator: (_rule: unknown, value: string, callback: (e?: Error) => void) => {
        const v = (value ?? '').trim()
        if (v === '') return callback()
        return /^1[3-9]\d{9}$/.test(v) ? callback() : callback(new Error('手机号格式不正确'))
      },
      trigger: 'blur',
    },
  ],
  qq: [
    {
      validator: (_rule: unknown, value: string, callback: (e?: Error) => void) => {
        const v = (value ?? '').trim()
        if (v === '') return callback()
        return /^[1-9]\d{4,10}$/.test(v) ? callback() : callback(new Error('QQ 号格式不正确'))
      },
      trigger: 'blur',
    },
  ],
  password: [
    {
      validator: (_rule: unknown, value: string, callback: (e?: Error) => void) => {
        if (form.id != null) return callback()
        const v = (value ?? '').trim()
        if (v === '') return callback(new Error('请输入初始密码'))
        if (v.length < 6 || v.length > 20) return callback(new Error('密码长度在 6 到 20 个字符'))
        return callback()
      },
      trigger: 'blur',
    },
  ],
})

const openAdd = async () => {
  suspendCascade = true
  Object.assign(form, emptyStudentForm())
  cascade.collegeId = null
  cascade.grade = null
  // 新增时学院/年级没选，先把全部班级拉出来，用户可以直接选班级
  formClasses.value = await loadClasses()
  dialogTitle.value = '新增学生'
  dialogVisible.value = true
  await nextTick()
  suspendCascade = false
  formRef.value?.clearValidate()
}

const openEdit = async (row: StudentVo) => {
  const id = row.id
  if (id == null) return

  suspendCascade = true
  Object.assign(form, {
    id,
    name: row.name,
    num: row.num,
    classId: row.classId ?? null,
    sex: row.sex ?? null,
    mobile: row.mobile ?? '',
    qq: row.qq ?? '',
    avatarUrl: row.avatarUrl ?? null,
    password: '',
  })
  // 学院 / 年级由列表行（join 出来的）带回来，用它俩把班级下拉先筛出来
  cascade.collegeId = row.collegeId ?? null
  cascade.grade = row.grade ?? null
  formClasses.value = await loadClasses(row.collegeId ?? undefined, row.grade ?? undefined)
  dialogTitle.value = '修改学生'
  dialogVisible.value = true
  await nextTick()
  suspendCascade = false
  formRef.value?.clearValidate()
}

/** el-upload 的自定义上传：走统一的 axios 实例，才能带上 Authorization 头 */
const handleUploadAvatar = async (options: UploadRequestOptions) => {
  const file = options.file
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('只能上传图片文件')
    return
  }
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.warning('头像不能超过 2MB')
    return
  }
  uploading.value = true
  try {
    form.avatarUrl = await uploadStudentPhoto(file, form.num)
    ElMessage.success('头像上传成功，保存后生效')
  } catch {
    // 失败提示由拦截器弹出
  } finally {
    uploading.value = false
  }
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const isAdd = form.id == null
    const payload: StudentForm = {
      id: form.id,
      name: form.name.trim(),
      num: form.num.trim(),
      classId: form.classId,
      sex: form.sex,
      mobile: form.mobile.trim(),
      qq: form.qq.trim(),
      avatarUrl: form.avatarUrl,
      // 修改时传空串：后端会当成 null 跳过，不动库里的密码
      password: isAdd ? form.password : '',
    }
    await saveStudent(payload)
    ElMessage.success(isAdd ? '添加成功' : '修改成功')
    dialogVisible.value = false
    await page.load()
  } catch {
    // 学号重复（50701）、班级不存在（50502）、无权限（403）等由拦截器提示
  } finally {
    submitting.value = false
  }
}

/* ---------------- 学生详情弹窗 ---------------- */

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<StudentVo | null>(null)

const openDetail = async (row: StudentVo) => {
  const id = row.id
  if (id == null) return
  detail.value = row
  detailVisible.value = true
  detailLoading.value = true
  try {
    // 列表数据可能已经过期，重新拉一次详情
    detail.value = await getStudentInfo(id)
  } catch {
    // 拉不到就沿用列表里的行数据
  } finally {
    detailLoading.value = false
  }
}

/* ---------------- 重置密码弹窗 ---------------- */

const pwdDialogVisible = ref(false)
const pwdSubmitting = ref(false)
const pwdFormRef = ref<FormInstance>()
const pwdTarget = ref<StudentVo | null>(null)
const pwdForm = reactive({ newPassword: '', confirmPassword: '' })

const pwdRules: FormRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' },
  ],
  confirmPassword: [{ required: true, message: '请再次输入新密码', trigger: 'blur' }],
}

const openResetPassword = async (row: StudentVo) => {
  pwdTarget.value = row
  pwdForm.newPassword = ''
  pwdForm.confirmPassword = ''
  pwdDialogVisible.value = true
  await nextTick()
  pwdFormRef.value?.clearValidate()
}

const handleResetPassword = async () => {
  const valid = await pwdFormRef.value?.validate().catch(() => false)
  if (!valid) return
  if (pwdForm.newPassword !== pwdForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  const id = pwdTarget.value?.id
  if (id == null) return

  pwdSubmitting.value = true
  try {
    await resetStudentPassword({ studentId: id, newPassword: pwdForm.newPassword })
    ElMessage.success('密码重置成功')
    pwdDialogVisible.value = false
  } catch {
    // 提示由拦截器弹出
  } finally {
    pwdSubmitting.value = false
  }
}
</script>

<template>
  <div class="student-page">
    <el-card shadow="never">
      <template #header>
        <span class="card-title">学生管理</span>
      </template>

      <!-- 查询栏 -->
      <el-form :inline="true" class="search-bar" @submit.prevent>
        <el-form-item label="学号">
          <el-input v-model="page.form.num" placeholder="支持模糊查询" clearable style="width: 150px"
            @keyup.enter="page.search" @clear="page.search" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="page.form.name" placeholder="支持模糊查询" clearable style="width: 140px"
            @keyup.enter="page.search" @clear="page.search" />
        </el-form-item>
        <el-form-item label="所属学院">
          <el-select v-model="page.form.collegeId" placeholder="全部" clearable style="width: 170px">
            <el-option v-for="college in baseInfoStore.colleges" :key="college.id" :label="college.name"
              :value="college.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="年级">
          <el-select v-model="page.form.grade" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="grade in baseInfoStore.grades" :key="grade.id" :label="`${grade.grade} 级`"
              :value="grade.grade" />
          </el-select>
        </el-form-item>
        <el-form-item label="班级">
          <el-select v-model="page.form.classId" placeholder="全部" clearable filterable style="width: 160px">
            <el-option v-for="clazz in filterClasses" :key="clazz.id" :label="clazz.num" :value="clazz.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="page.search">查询</el-button>
          <el-button :icon="Refresh" @click="page.reset">重置</el-button>
          <el-button v-if="loginStore.hasPermission(PERM_ADD)" type="success" :icon="Plus" @click="openAdd">
            新增学生
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 列表 -->
      <el-table :data="page.records" v-loading="page.loading" border stripe>
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column label="头像" width="80" align="center">
          <template #default="{ row }">
            <el-avatar :size="36" :src="avatarSrc(row.avatarUrl)">
              {{ row.name?.charAt(0) || '?' }}
            </el-avatar>
          </template>
        </el-table-column>
        <el-table-column prop="num" label="学号" width="130" show-overflow-tooltip />
        <el-table-column prop="name" label="姓名" width="110" show-overflow-tooltip />
        <el-table-column label="班级" width="130" align="center">
          <template #default="{ row }">{{ row.className || '-' }}</template>
        </el-table-column>
        <el-table-column label="所属学院" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.collegeName || '-' }}</template>
        </el-table-column>
        <el-table-column label="年级" width="100" align="center">
          <template #default="{ row }">{{ row.grade == null ? '-' : `${row.grade} 级` }}</template>
        </el-table-column>
        <el-table-column label="性别" width="80" align="center">
          <template #default="{ row }">{{ sexLabel(row.sex) }}</template>
        </el-table-column>
        <el-table-column label="手机号" width="130" align="center">
          <template #default="{ row }">{{ row.mobile || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <el-button v-if="loginStore.hasPermission(PERM_DETAIL)" link type="info" @click="openDetail(row)">
              详情
            </el-button>
            <el-button v-if="loginStore.hasPermission(PERM_UPDATE)" link type="primary" @click="openEdit(row)">
              修改
            </el-button>
            <el-button v-if="loginStore.hasPermission(PERM_PWD)" link type="warning"
              @click="openResetPassword(row)">
              重置密码
            </el-button>
            <span
              v-if="!loginStore.hasPermission(PERM_DETAIL) && !loginStore.hasPermission(PERM_UPDATE) && !loginStore.hasPermission(PERM_PWD)"
              class="no-perm">无操作权限</span>
          </template>
        </el-table-column>
        <template #empty>暂无数据</template>
      </el-table>

      <Pagination v-model:current="page.current" v-model:size="page.size" :total="page.total"
        @change="page.onPageChange" />
    </el-card>

    <!-- 新增 / 修改弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="540px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="所属学院">
          <!-- 学院/年级不提交，只是用来联动筛班级：库表里学生只有 classId -->
          <el-select v-model="cascade.collegeId" placeholder="全部学院" clearable style="width: 100%">
            <el-option v-for="college in baseInfoStore.colleges" :key="college.id" :label="college.name"
              :value="college.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="年级">
          <el-select v-model="cascade.grade" placeholder="全部年级" clearable style="width: 100%">
            <el-option v-for="grade in baseInfoStore.grades" :key="grade.id" :label="`${grade.grade} 级`"
              :value="grade.grade" />
          </el-select>
        </el-form-item>
        <el-form-item label="班级" prop="classId">
          <el-select v-model="form.classId" placeholder="请选择班级" filterable style="width: 100%">
            <el-option v-for="clazz in formClasses" :key="clazz.id" :label="clazz.num" :value="clazz.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="学号" prop="num">
          <el-input v-model="form.num" placeholder="如 2025001" maxlength="12" />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="请输入姓名" maxlength="50" />
        </el-form-item>
        <el-form-item label="性别" prop="sex">
          <el-select v-model="form.sex" placeholder="请选择性别" style="width: 100%">
            <el-option v-for="sex in baseInfoStore.sexTypes" :key="sex.id" :label="sex.name" :value="sex.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号" prop="mobile">
          <el-input v-model="form.mobile" placeholder="可留空" maxlength="11" />
        </el-form-item>
        <el-form-item label="QQ" prop="qq">
          <el-input v-model="form.qq" placeholder="可留空" maxlength="11" />
        </el-form-item>
        <el-form-item label="头像">
          <div class="avatar-upload">
            <el-avatar :size="64" :src="avatarSrc(form.avatarUrl)">
              {{ form.name?.charAt(0) || '?' }}
            </el-avatar>
            <el-upload :show-file-list="false" :http-request="handleUploadAvatar" accept="image/*">
              <el-button :icon="Upload" :loading="uploading">选择图片</el-button>
            </el-upload>
            <span class="upload-tip">不超过 2MB，保存后生效</span>
          </div>
        </el-form-item>
        <el-form-item v-if="form.id == null" label="初始密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="6-20 个字符" show-password
            maxlength="20" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 学生详情弹窗 -->
    <el-dialog v-model="detailVisible" title="学生详情" width="560px">
      <div v-loading="detailLoading">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="学号">{{ detail?.num || '-' }}</el-descriptions-item>
          <el-descriptions-item label="姓名">{{ detail?.name || '-' }}</el-descriptions-item>
          <el-descriptions-item label="所属学院">{{ detail?.collegeName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="年级">
            {{ detail?.grade == null ? '-' : `${detail.grade} 级` }}
          </el-descriptions-item>
          <el-descriptions-item label="班级">{{ detail?.className || '-' }}</el-descriptions-item>
          <el-descriptions-item label="性别">{{ sexLabel(detail?.sex) }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ detail?.mobile || '-' }}</el-descriptions-item>
          <el-descriptions-item label="QQ">{{ detail?.qq || '-' }}</el-descriptions-item>
          <el-descriptions-item label="头像" :span="2">
            <el-avatar :size="56" :src="avatarSrc(detail?.avatarUrl)">
              {{ detail?.name?.charAt(0) || '?' }}
            </el-avatar>
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码弹窗 -->
    <el-dialog v-model="pwdDialogVisible" title="重置密码" width="420px" :close-on-click-modal="false">
      <p class="dialog-tip">学生：{{ pwdTarget?.name }}（{{ pwdTarget?.num }}）</p>
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="100px">
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" placeholder="6-20 个字符" show-password
            maxlength="20" />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password
            maxlength="20" @keyup.enter="handleResetPassword" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="pwdSubmitting" @click="handleResetPassword">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.student-page {
  min-height: 100%;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
}

.search-bar {
  margin-bottom: 4px;
}

.search-bar :deep(.el-form-item) {
  margin-bottom: 12px;
}

.no-perm {
  color: #909399;
  font-size: 12px;
}

.avatar-upload {
  display: flex;
  align-items: center;
  gap: 12px;
}

.upload-tip {
  color: #909399;
  font-size: 12px;
}

.dialog-tip {
  margin: 0 0 12px;
  color: #606266;
  font-size: 13px;
}
</style>
