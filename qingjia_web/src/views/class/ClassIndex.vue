<script setup lang="ts">
/**
 * 班级管理（菜单「基础信息 -> 班级管理」，url = /class/index）。
 *
 * 结构和 CollegeIndex.vue 一致：usePageTable 管查询条件 + 分页 + loading，
 * loginStore.hasPermission 管按钮显隐，增删改后局部刷新基础数据字典。
 *
 * 对着老 Thymeleaf 版 templates/class/list.html 搬过来的差异：
 * 1. 老页面查询条件放在 <form action="/class/index" method="get"> 里，点图片按钮整页提交；
 *    这里改成 SPA 的异步查询，条件变了只重查列表。
 * 2. 老页面的「指定班主任」调 /class/searchTeachers，但那个路径只存在于老的
 *    ClassController 里，JSON 版是 /api/class/teachers —— 所以这里用的是新接口。
 * 3. 老页面每次保存完 window.location.reload()，这里改成只重查当前页，
 *    用户停在的页码和查询条件都不会丢。
 */
import { nextTick, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import Pagination from '@/components/Pagination.vue'
import { usePageTable } from '@/composables/usePageTable'
import { useLoginStore } from '@/stores/loginStores'
import { useBaseInfoStore } from '@/stores/baseInfoStore'
import {
  deleteClass,
  getClassInfo,
  getClassList,
  saveClass,
  searchTeachersApi,
  setHeadTeacherApi,
} from '@/api/classApi'
import { emptyClassForm, type ClassForm, type ClassVo } from '@/type/classPojo'
import type { TeacherOption } from '@/type/Teacher'

const loginStore = useLoginStore()
const baseInfoStore = useBaseInfoStore()

// 权限串必须和后端 ApiClassController 上的 @RequiresPermissions 完全一致。
// 按钮隐藏只是体验优化，真正的拦截在后端。
const PERM_ADD = 'baseinfo:class:add'
const PERM_UPDATE = 'baseinfo:class:update'
const PERM_DELETE = 'baseinfo:class:delete'
const PERM_BANZHUREN = 'baseinfo:class:banzhuren'

// 查询条件写成必填 string / 可选 number：el-input 的 v-model 需要确定的 string，
// el-select 用 undefined 表示「全部」（axios 会把 undefined 的参数整个丢掉，
// 于是后端收到 null，等于不加这个过滤条件）。
const page = usePageTable<ClassVo, { num: string; belongToCollegeId?: number; grade?: number }>(
  (params) => getClassList(params),
  { query: { num: '', belongToCollegeId: undefined, grade: undefined } },
)

/* ---------------- 新增 / 修改弹窗 ---------------- */

const dialogVisible = ref(false)
const dialogTitle = ref('新增班级')
const submitting = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<ClassForm>(emptyClassForm())

const rules: FormRules = {
  num: [
    { required: true, message: '请输入班级编号', trigger: 'blur' },
    // class.num 是 varchar(10)，后端 ClassPojo 上没有长度校验，
    // 前端不挡住的话超长会变成 DB 层的报错（用户只看到一句“服务器错误”）。
    { max: 10, message: '班级编号不超过 10 个字符', trigger: 'blur' },
  ],
  belongToCollegeId: [{ required: true, message: '请选择所属学院', trigger: 'change' }],
  grade: [{ required: true, message: '请选择年级', trigger: 'change' }],
}

const openAdd = async () => {
  Object.assign(form, emptyClassForm())
  dialogTitle.value = '新增班级'
  dialogVisible.value = true
  // 弹窗是复用的，上一次留下的校验红字必须清掉
  await nextTick()
  formRef.value?.clearValidate()
}

const openEdit = async (row: ClassVo) => {
  // ClassVo.id 是可选的（新增场景下前端拿不到 id），列表行一定有值，
  // 这里显式挡一下，避免 undefined 被当成「新增」提交上去变成插一条新记录。
  const id = row.id
  if (id == null) return

  Object.assign(form, {
    id,
    num: row.num,
    belongToCollegeId: row.belongToCollegeId ?? null,
    grade: row.grade ?? null,
    // 先把列表行里的班主任带过来，再拉一次详情覆盖，理由见 classPojo.ts 里
    // ClassForm.banZhuRenId 的注释：漏传这个字段等于把班主任清掉。
    banZhuRenId: row.banZhuRenId ?? null,
  })
  dialogTitle.value = '修改班级'
  dialogVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()

  try {
    const latest = await getClassInfo(id)
    Object.assign(form, {
      num: latest.num,
      belongToCollegeId: latest.belongToCollegeId ?? null,
      grade: latest.grade ?? null,
      banZhuRenId: latest.banZhuRenId ?? null,
    })
  } catch {
    // 拉不到就沿用列表里的值，错误提示 request.ts 的拦截器已经弹过了
  }
}

const handleSubmit = async () => {
  // validate() 校验不通过时是 reject，这里 catch 成 false，避免抛出未捕获的 rejection
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const payload: ClassForm = {
      id: form.id,
      num: form.num.trim(),
      belongToCollegeId: form.belongToCollegeId,
      grade: form.grade,
      banZhuRenId: form.banZhuRenId,
    }
    const isAdd = payload.id == null
    await saveClass(payload)
    ElMessage.success(isAdd ? '新增成功' : '修改成功')
    dialogVisible.value = false
    // 新增的记录排在表的末尾，当前页不一定看得见，重新按当前条件查一次即可；
    // 用 load 而不是 search，是为了不把用户翻到的页码踢回第一页。
    await page.load()
  } catch {
    // 编号重复（50501）、学院不存在（50202）、无权限（403）等由拦截器提示，这里不重复弹
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (row: ClassVo) => {
  const id = row.id
  if (id == null) return

  try {
    await ElMessageBox.confirm(`确定删除班级「${row.num}」吗？删除后无法恢复。`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return // 用户点了取消
  }

  try {
    await deleteClass(id)
    ElMessage.success('删除成功')
    // 删的正好是当前页最后一条时会自动往前退一页，不会停在空页上
    await page.reloadAfterDelete()
  } catch {
    // 班上有学生时后端会拒绝（50504），提示由拦截器弹出
  }
}

/* ---------------- 指定班主任弹窗 ---------------- */

const teacherDialogVisible = ref(false)
const teacherLoading = ref(false)
const teacherSaving = ref(false)
const teacherKeyword = ref('')
const teacherOptions = ref<TeacherOption[]>([])
const selectedTeacherId = ref<number | null>(null)
/** 正在改的班级行，用来在弹窗顶部显示「XX班 当前班主任：YY」 */
const currentClass = ref<ClassVo | null>(null)
/** 输入防抖，避免每敲一个字符就打一次搜索请求 */
let searchTimer: ReturnType<typeof setTimeout> | null = null

const openTeacherDialog = async (row: ClassVo) => {
  if (row.id == null) return
  currentClass.value = row
  // 预选中当前班主任，用户不改直接点保存也不会误清空
  selectedTeacherId.value = row.banZhuRenId ?? null
  teacherKeyword.value = ''
  teacherOptions.value = []
  teacherDialogVisible.value = true
  // 弹窗一打开就按当前班主任的姓名搜一次，省得用户还要自己重敲一遍
  if (row.bzrName) {
    teacherKeyword.value = row.bzrName
    await loadTeachers(row.bzrName)
  }
}

const loadTeachers = async (keyword: string) => {
  const name = keyword.trim()
  if (!name) {
    // 后端 name 是 required=false，空关键字会把全校教师都返回回来，
    // 既慢又没什么用，所以这里直接不发请求。
    teacherOptions.value = []
    return
  }
  teacherLoading.value = true
  try {
    teacherOptions.value = await searchTeachersApi(name)
  } catch {
    teacherOptions.value = []
  } finally {
    teacherLoading.value = false
  }
}

// 输入即搜（300ms 防抖）；点搜索按钮走的是同一个 loadTeachers
watch(teacherKeyword, (value) => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    void loadTeachers(value)
  }, 300)
})

const handleSetHeadTeacher = async () => {
  const classId = currentClass.value?.id
  const teacherId = selectedTeacherId.value
  if (classId == null) return
  if (teacherId == null) {
    ElMessage.warning('请先搜索并选择一位教师')
    return
  }

  teacherSaving.value = true
  try {
    await setHeadTeacherApi({ classId, teacherId })
    ElMessage.success('指定班主任成功')
    teacherDialogVisible.value = false
    await page.load()
  } catch {
    // 班主任角色不存在（50503）、教师不存在（50602）等由拦截器提示
  } finally {
    teacherSaving.value = false
  }
}
</script>

<template>
  <div class="class-page">
    <el-card shadow="never">
      <template #header>
        <span class="card-title">班级管理</span>
      </template>

      <!-- 查询栏 -->
      <el-form :inline="true" class="search-bar" @submit.prevent>
        <el-form-item label="班级编号">
          <el-input v-model="page.form.num" placeholder="支持模糊查询" clearable style="width: 160px"
            @keyup.enter="page.search" @clear="page.search" />
        </el-form-item>
        <el-form-item label="所属学院">
          <el-select v-model="page.form.belongToCollegeId" placeholder="全部" clearable style="width: 180px">
            <el-option v-for="college in baseInfoStore.colleges" :key="college.id" :label="college.name"
              :value="college.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="年级">
          <el-select v-model="page.form.grade" placeholder="全部" clearable style="width: 130px">
            <el-option v-for="grade in baseInfoStore.grades" :key="grade.id" :label="`${grade.grade} 级`"
              :value="grade.grade" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="page.search">查询</el-button>
          <el-button :icon="Refresh" @click="page.reset">重置</el-button>
          <el-button v-if="loginStore.hasPermission(PERM_ADD)" type="success" :icon="Plus" @click="openAdd">
            新增班级
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 列表 -->
      <el-table :data="page.records" v-loading="page.loading" border stripe>
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="num" label="班级编号" min-width="140" show-overflow-tooltip />
        <el-table-column label="所属学院" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.collegeName || '-' }}</template>
        </el-table-column>
        <el-table-column label="年级" width="110" align="center">
          <template #default="{ row }">{{ row.grade == null ? '-' : `${row.grade} 级` }}</template>
        </el-table-column>
        <el-table-column label="班主任" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.bzrName">{{ row.bzrName }}</span>
            <span v-else class="no-perm">未指定</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" align="center" fixed="right">
          <template #default="{ row }">
            <el-button v-if="loginStore.hasPermission(PERM_BANZHUREN)" link type="warning"
              @click="openTeacherDialog(row)">
              指定班主任
            </el-button>
            <el-button v-if="loginStore.hasPermission(PERM_UPDATE)" link type="primary" @click="openEdit(row)">
              修改
            </el-button>
            <el-button v-if="loginStore.hasPermission(PERM_DELETE)" link type="danger" @click="handleDelete(row)">
              删除
            </el-button>
            <span
              v-if="!loginStore.hasPermission(PERM_UPDATE) && !loginStore.hasPermission(PERM_DELETE) && !loginStore.hasPermission(PERM_BANZHUREN)"
              class="no-perm">无操作权限</span>
          </template>
        </el-table-column>
        <template #empty>暂无数据</template>
      </el-table>

      <Pagination v-model:current="page.current" v-model:size="page.size" :total="page.total"
        @change="page.onPageChange" />
    </el-card>

    <!-- 新增 / 修改弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="480px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="班级编号" prop="num">
          <el-input v-model="form.num" placeholder="如 03202003" maxlength="10" />
        </el-form-item>
        <el-form-item label="所属学院" prop="belongToCollegeId">
          <el-select v-model="form.belongToCollegeId" placeholder="请选择学院" style="width: 100%">
            <el-option v-for="college in baseInfoStore.colleges" :key="college.id" :label="college.name"
              :value="college.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="年级" prop="grade">
          <el-select v-model="form.grade" placeholder="请选择年级" style="width: 100%">
            <el-option v-for="grade in baseInfoStore.grades" :key="grade.id" :label="`${grade.grade} 级`"
              :value="grade.grade" />
          </el-select>
        </el-form-item>
        <!--
          班主任不在这个表单里改：后端换班主任要顺带维护 user_role 表里的
          「班主任」角色，只能走 /api/class/setHeadTeacher。这里只是把原值
          原样带回去（form.banZhuRenId），避免后端把「没传」当成「要清空」。
        -->
        <el-form-item label="班主任">
          <span>{{ form.id == null ? '保存后可用「指定班主任」设置' : (form.banZhuRenId == null ? '未指定' : '已指定') }}</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 指定班主任弹窗 -->
    <el-dialog v-model="teacherDialogVisible" title="指定班主任" width="520px" :close-on-click-modal="false">
      <p class="teacher-tip">
        班级：{{ currentClass?.num }}
        <span class="teacher-tip-sep">|</span>
        当前班主任：{{ currentClass?.bzrName || '未指定' }}
      </p>
      <el-input v-model="teacherKeyword" placeholder="输入教师姓名进行搜索" clearable :prefix-icon="Search"
        @keyup.enter="loadTeachers(teacherKeyword)" />

      <div class="teacher-list" v-loading="teacherLoading">
        <el-empty v-if="!teacherKeyword.trim()" description="请输入教师姓名搜索" :image-size="60" />
        <el-empty v-else-if="teacherOptions.length === 0" description="未找到相关教师" :image-size="60" />
        <el-radio-group v-else v-model="selectedTeacherId" class="teacher-radio-group">
          <el-radio v-for="teacher in teacherOptions" :key="teacher.id" :value="teacher.id" class="teacher-radio">
            <span class="teacher-name">{{ teacher.name }}</span>
            <span class="teacher-college">{{ teacher.collegeName || '-' }}</span>
          </el-radio>
        </el-radio-group>
      </div>

      <template #footer>
        <el-button @click="teacherDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="teacherSaving" @click="handleSetHeadTeacher">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.class-page {
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

.teacher-tip {
  margin: 0 0 12px;
  color: #606266;
  font-size: 13px;
}

.teacher-tip-sep {
  margin: 0 8px;
  color: #dcdfe6;
}

.teacher-list {
  margin-top: 12px;
  max-height: 280px;
  min-height: 120px;
  overflow-y: auto;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 8px;
}

.teacher-radio-group {
  display: flex;
  flex-direction: column;
  width: 100%;
}

.teacher-radio {
  height: 40px;
  margin-right: 0;
}

.teacher-name {
  margin-right: 10px;
  font-weight: 600;
}

.teacher-college {
  color: #909399;
  font-size: 12px;
}
</style>
