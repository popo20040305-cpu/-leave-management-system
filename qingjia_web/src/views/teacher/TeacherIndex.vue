<script setup lang="ts">
/**
 * 教师管理（菜单「基础信息 -> 教师管理」，url = /teacher/index）。
 *
 * 对着老 Thymeleaf 版 templates/teacher/teacher.html 搬的：查询条件（工号 / 姓名 /
 * 所属学院）、表格 + 操作列、新增/修改弹窗、重置密码、指定教师角色。
 *
 * 与老页面的差异：
 * 1. 老页面保存走 multipart（teacherJson 字符串 + photo 文件）一个请求；这里拆成
 *    两步 —— 头像先单独传到 /api/teacher/uploadPhoto 拿到相对路径，再随表单一并提交。
 *    这样保存失败时不会连头像一起白传，表单校验和提交逻辑也不用拼 FormData。
 * 2. 老页面每次保存完 window.location.reload()，这里只重查当前页，页码和条件都不丢。
 * 3. 修改时不会把密码 / 头像清空：后端 updateById 用的是 NOT_NULL 字段策略，
 *    密码填 null 会被跳过（见 api/teacherApi.ts 的注释）。
 */
import { computed, nextTick, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules, UploadRequestOptions } from 'element-plus'
import { Plus, Refresh, Search, Upload } from '@element-plus/icons-vue'
import Pagination from '@/components/Pagination.vue'
import { usePageTable } from '@/composables/usePageTable'
import { useLoginStore } from '@/stores/loginStores'
import { useBaseInfoStore } from '@/stores/baseInfoStore'
import {
  deleteTeacher,
  getTeacherInfo,
  getTeacherList,
  getTeacherRoles,
  resetTeacherPassword,
  saveTeacher,
  saveTeacherRoles,
  uploadTeacherPhoto,
} from '@/api/teacherApi'
import { emptyTeacherForm, type TeacherForm, type TeacherVo } from '@/type/teacherPojo'
import { BASE_URL } from '@/utils/config'

const loginStore = useLoginStore()
const baseInfoStore = useBaseInfoStore()

// 权限串必须和后端 ApiTeacherController 上的 @RequiresPermissions 完全一致
const PERM_ADD = 'baseinfo:teacher:add'
const PERM_UPDATE = 'baseinfo:teacher:update'
const PERM_DELETE = 'baseinfo:teacher:delete'
const PERM_PWD = 'baseinfo:teacher:updatepassword'
const PERM_ROLE = 'baseinfo:teacher:zdjs'

const page = usePageTable<TeacherVo, { num: string; name: string; collegeId?: number }>(
  (params) => getTeacherList(params),
  { query: { num: '', name: '', collegeId: undefined } },
)

/**
 * 头像地址：后端返回的是 /teacherPhoto/xxx.jpg 这种相对路径，
 * 静态资源挂在 context-path（/qingjia）下，所以要拼上 BASE_URL 才能显示。
 */
const avatarSrc = (url?: string | null) => (url ? `${BASE_URL}${url}` : '')

/* ---------------- 新增 / 修改弹窗 ---------------- */

const dialogVisible = ref(false)
const dialogTitle = ref('新增教师')
const submitting = ref(false)
const uploading = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<TeacherForm>(emptyTeacherForm())

const rules = computed<FormRules>(() => ({
  num: [
    { required: true, message: '请输入工号', trigger: 'blur' },
    { max: 20, message: '工号不超过 20 个字符', trigger: 'blur' },
  ],
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { max: 50, message: '姓名不超过 50 个字符', trigger: 'blur' },
  ],
  collegeId: [{ required: true, message: '请选择所属学院', trigger: 'change' }],
  mobile: [
    {
      // 后端 TeacherPojo.mobile 上没有 @Pattern，但库里存的是手机号，
      // 前端挡一下格式，避免把「随便一串字符」存进去后没法按手机号找人。
      // 空串放行：留空表示没填。
      validator: (_rule: unknown, value: string, callback: (e?: Error) => void) => {
        const v = (value ?? '').trim()
        if (v === '') return callback()
        return /^1[3-9]\d{9}$/.test(v) ? callback() : callback(new Error('手机号格式不正确'))
      },
      trigger: 'blur',
    },
  ],
  password: [
    {
      // 密码只在新增时必填；修改时留空 = 不改密码
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
}))

const openAdd = async () => {
  Object.assign(form, emptyTeacherForm())
  dialogTitle.value = '新增教师'
  dialogVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}

const openEdit = async (row: TeacherVo) => {
  const id = row.id
  if (id == null) return

  Object.assign(form, {
    id,
    name: row.name,
    num: row.num,
    collegeId: row.collegeId ?? null,
    mobile: row.mobile ?? '',
    avatarUrl: row.avatarUrl ?? null,
    password: '',
  })
  dialogTitle.value = '修改教师'
  dialogVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()

  // 先用列表行把弹窗填上，再拉一次详情覆盖（列表可能是几分钟前查的）
  try {
    const latest = await getTeacherInfo(id)
    Object.assign(form, {
      name: latest.name,
      num: latest.num,
      collegeId: latest.collegeId ?? null,
      mobile: latest.mobile ?? '',
      avatarUrl: latest.avatarUrl ?? null,
    })
  } catch {
    // 拉不到就沿用列表里的值，提示由 request.ts 的拦截器弹出
  }
}

/** el-upload 的自定义上传：不走默认 XHR，改用统一的 axios 实例（带 token） */
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
    form.avatarUrl = await uploadTeacherPhoto(file, form.num)
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
    const payload: TeacherForm = {
      id: form.id,
      name: form.name.trim(),
      num: form.num.trim(),
      collegeId: form.collegeId,
      mobile: form.mobile.trim(),
      avatarUrl: form.avatarUrl,
      // 修改时 password 传空串：后端会当成 null 跳过，不动库里的密码
      password: isAdd ? form.password : '',
    }
    await saveTeacher(payload)
    ElMessage.success(isAdd ? '添加成功' : '修改成功')
    dialogVisible.value = false
    await page.load()
  } catch {
    // 工号重复（50601）、学院不存在（50202）、无权限（403）等由拦截器提示
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (row: TeacherVo) => {
  const id = row.id
  if (id == null) return

  try {
    await ElMessageBox.confirm(`确定删除教师「${row.name}」吗？删除后无法恢复。`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return // 用户点了取消
  }

  try {
    await deleteTeacher(id)
    ElMessage.success('删除成功')
    await page.reloadAfterDelete()
  } catch {
    // 还是某个班的班主任时后端会拒绝（50603），提示由拦截器弹出
  }
}

/* ---------------- 重置密码弹窗 ---------------- */

const pwdDialogVisible = ref(false)
const pwdSubmitting = ref(false)
const pwdFormRef = ref<FormInstance>()
const pwdTarget = ref<TeacherVo | null>(null)
const pwdForm = reactive({ newPassword: '', confirmPassword: '' })

const pwdRules: FormRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' },
  ],
  confirmPassword: [{ required: true, message: '请再次输入新密码', trigger: 'blur' }],
}

const openResetPassword = async (row: TeacherVo) => {
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
    await resetTeacherPassword({ teacherId: id, newPassword: pwdForm.newPassword })
    ElMessage.success('密码重置成功')
    pwdDialogVisible.value = false
  } catch {
    // 提示由拦截器弹出
  } finally {
    pwdSubmitting.value = false
  }
}

/* ---------------- 指定教师角色弹窗 ---------------- */

const roleDialogVisible = ref(false)
const roleLoading = ref(false)
const roleSubmitting = ref(false)
const roleTarget = ref<TeacherVo | null>(null)
const roleOptions = ref<{ id?: number; name: string; remark?: string | null }[]>([])
const checkedRoleIds = ref<number[]>([])

const openRoles = async (row: TeacherVo) => {
  const id = row.id
  if (id == null) return
  roleTarget.value = row
  roleDialogVisible.value = true
  roleLoading.value = true
  try {
    const data = await getTeacherRoles(id)
    roleOptions.value = data.allRoles
    checkedRoleIds.value = data.checkedIds
  } catch {
    roleOptions.value = []
    checkedRoleIds.value = []
  } finally {
    roleLoading.value = false
  }
}

const handleSaveRoles = async () => {
  const id = roleTarget.value?.id
  if (id == null) return
  roleSubmitting.value = true
  try {
    await saveTeacherRoles(id, checkedRoleIds.value)
    ElMessage.success('指定教师角色成功')
    roleDialogVisible.value = false
  } catch {
    // 提示由拦截器弹出
  } finally {
    roleSubmitting.value = false
  }
}
</script>

<template>
  <div class="teacher-page">
    <el-card shadow="never">
      <template #header>
        <span class="card-title">教师管理</span>
      </template>

      <!-- 查询栏 -->
      <el-form :inline="true" class="search-bar" @submit.prevent>
        <el-form-item label="工号">
          <el-input v-model="page.form.num" placeholder="支持模糊查询" clearable style="width: 150px"
            @keyup.enter="page.search" @clear="page.search" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="page.form.name" placeholder="支持模糊查询" clearable style="width: 150px"
            @keyup.enter="page.search" @clear="page.search" />
        </el-form-item>
        <el-form-item label="所属学院">
          <el-select v-model="page.form.collegeId" placeholder="全部" clearable style="width: 180px">
            <el-option v-for="college in baseInfoStore.colleges" :key="college.id" :label="college.name"
              :value="college.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="page.search">查询</el-button>
          <el-button :icon="Refresh" @click="page.reset">重置</el-button>
          <el-button v-if="loginStore.hasPermission(PERM_ADD)" type="success" :icon="Plus" @click="openAdd">
            新增教师
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
        <el-table-column prop="num" label="工号" width="130" show-overflow-tooltip />
        <el-table-column prop="name" label="姓名" width="130" show-overflow-tooltip />
        <el-table-column label="所属学院" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.collegeName || '-' }}</template>
        </el-table-column>
        <el-table-column label="手机号" width="140" align="center">
          <template #default="{ row }">{{ row.mobile || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="280" align="center" fixed="right">
          <template #default="{ row }">
            <el-button v-if="loginStore.hasPermission(PERM_UPDATE)" link type="primary" @click="openEdit(row)">
              修改
            </el-button>
            <el-button v-if="loginStore.hasPermission(PERM_PWD)" link type="warning"
              @click="openResetPassword(row)">
              重置密码
            </el-button>
            <el-button v-if="loginStore.hasPermission(PERM_ROLE)" link type="info" @click="openRoles(row)">
              指定角色
            </el-button>
            <el-button v-if="loginStore.hasPermission(PERM_DELETE)" link type="danger" @click="handleDelete(row)">
              删除
            </el-button>
            <span
              v-if="!loginStore.hasPermission(PERM_UPDATE) && !loginStore.hasPermission(PERM_PWD) && !loginStore.hasPermission(PERM_ROLE) && !loginStore.hasPermission(PERM_DELETE)"
              class="no-perm">无操作权限</span>
          </template>
        </el-table-column>
        <template #empty>暂无数据</template>
      </el-table>

      <Pagination v-model:current="page.current" v-model:size="page.size" :total="page.total"
        @change="page.onPageChange" />
    </el-card>

    <!-- 新增 / 修改弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="工号" prop="num">
          <el-input v-model="form.num" placeholder="如 1001" maxlength="20" />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="请输入姓名" maxlength="50" />
        </el-form-item>
        <el-form-item label="所属学院" prop="collegeId">
          <el-select v-model="form.collegeId" placeholder="请选择学院" style="width: 100%">
            <el-option v-for="college in baseInfoStore.colleges" :key="college.id" :label="college.name"
              :value="college.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号" prop="mobile">
          <el-input v-model="form.mobile" placeholder="可留空" maxlength="11" />
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

    <!-- 重置密码弹窗 -->
    <el-dialog v-model="pwdDialogVisible" title="重置密码" width="420px" :close-on-click-modal="false">
      <p class="dialog-tip">教师：{{ pwdTarget?.name }}（{{ pwdTarget?.num }}）</p>
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

    <!-- 指定教师角色弹窗 -->
    <el-dialog v-model="roleDialogVisible" title="指定教师角色" width="480px" :close-on-click-modal="false">
      <p class="dialog-tip">教师：{{ roleTarget?.name }}（{{ roleTarget?.num }}）</p>
      <div v-loading="roleLoading" class="role-list">
        <el-empty v-if="!roleLoading && roleOptions.length === 0" description="没有可分配的角色" :image-size="60" />
        <el-checkbox-group v-else v-model="checkedRoleIds" class="role-group">
          <el-checkbox v-for="role in roleOptions" :key="role.id" :value="role.id" class="role-item">
            <span class="role-name">{{ role.name }}</span>
            <span v-if="role.remark" class="role-remark">{{ role.remark }}</span>
          </el-checkbox>
        </el-checkbox-group>
      </div>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="roleSubmitting" @click="handleSaveRoles">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.teacher-page {
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

.role-list {
  min-height: 80px;
}

.role-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.role-item {
  height: auto;
  margin-right: 0;
  padding: 4px 0;
}

.role-name {
  margin-right: 8px;
}

.role-remark {
  color: #909399;
  font-size: 12px;
}
</style>
