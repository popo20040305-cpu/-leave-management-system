<script setup lang="ts">
/**
 * 学院管理（菜单「基础信息 -> 学院管理」，url = /college/index）。
 *
 * 这是第一个真正落地的业务页，公共模块的三样东西都在这里用上了：
 * - usePageTable：查询条件 + 分页 + loading + 拉数据
 * - Pagination：分页条
 * - loginStore.hasPermission：按权限显隐「新增 / 修改 / 删除」按钮
 *
 * 页面结构对着老 Thymeleaf 版 templates/college/list.html 搬的：
 * 查询条件（学院名称 / 学院编号，都是模糊匹配）、表格四列 + 操作列、
 * 新增/修改共用一个弹窗、删除前二次确认。
 */
import { nextTick, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import Pagination from '@/components/Pagination.vue'
import { usePageTable } from '@/composables/usePageTable'
import { useLoginStore } from '@/stores/loginStores'
import { useBaseInfoStore } from '@/stores/baseInfoStore'
import {
  deleteCollege,
  getCollegeInfo,
  getCollegeList,
  saveCollege,
} from '@/api/collegeApi'
import { emptyCollegeForm, type College, type CollegeForm } from '@/type/collegePojo'

const loginStore = useLoginStore()
const baseInfoStore = useBaseInfoStore()

// 权限串必须和后端 ApiCollegeController 上的 @RequiresPermissions 完全一致。
// 按钮隐藏只是体验优化，真正的拦截在后端；两边不同步会出现
// “按钮看得见但点了报没权限”，或者“有权限却没按钮”。
const PERM_ADD = 'baseinfo:college:add'
const PERM_UPDATE = 'baseinfo:college:update'
const PERM_DELETE = 'baseinfo:college:delete'

// 查询条件故意写成必填的 string 而不是 CollegeQuery（两个字段都是可选的）：
// 搜索框的 v-model 需要一个确定的 string，可选类型会让 el-input 拿到 undefined，
// 输入时表现为「第一个字符吞掉」。发请求时 { name: string } 可以赋给
// { name?: string }，所以类型上是兼容的。
const page = usePageTable<College, { name: string; num: string }>(
  (params) => getCollegeList(params),
  { query: { name: '', num: '' } },
)

/* ---------------- 新增 / 修改弹窗 ---------------- */

const dialogVisible = ref(false)
const dialogTitle = ref('新增学院')
const submitting = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<CollegeForm>(emptyCollegeForm())

const rules: FormRules = {
  name: [
    { required: true, message: '请输入学院名称', trigger: 'blur' },
    { max: 50, message: '学院名称不超过 50 个字符', trigger: 'blur' },
  ],
  num: [
    { required: true, message: '请输入学院编号', trigger: 'blur' },
    { max: 20, message: '学院编号不超过 20 个字符', trigger: 'blur' },
  ],
  tel: [
    {
      // 必须和后端 CollegePojo.tel 上的 @Pattern 保持一致，否则会出现
      // “前端放过去了、后端校验失败”或者反过来的情况。
      // 后端现在是 ^$|^[0-9 ()\-+]{3,20}$：允许留空，否则 3-20 位的
      // 数字 / 空格 / 圆括号 / 加号 / 连字符（库里就有 '0718 - 8437432' 这种带分隔符的号码）。
      // el-form 的 pattern 规则对空串也会跑正则，直接写 pattern 会导致
      // “没填电话却提示格式不正确”，所以这里用自定义校验器放过空值。
      validator: (_rule: unknown, value: string, callback: (e?: Error) => void) => {
        const v = (value ?? '').trim()
        if (v === '') return callback()
        return /^[0-9 ()\-+]{3,20}$/.test(v)
          ? callback()
          : callback(new Error('办公电话为 3-20 位，可含空格、-、()、+'))
      },
      trigger: 'blur',
    },
  ],
}

const openAdd = async () => {
  Object.assign(form, emptyCollegeForm())
  dialogTitle.value = '新增学院'
  dialogVisible.value = true
  // 弹窗是复用的，上一次留下的校验红字必须清掉
  await nextTick()
  formRef.value?.clearValidate()
}

const openEdit = async (row: College) => {
  // College.id 是可选的（新增场景下前端拿不到 id），列表行一定有值，
  // 这里显式挡一下，避免 undefined 被当成「新增」提交上去变成插一条新记录。
  const id = row.id
  if (id == null) return

  Object.assign(form, { id, name: row.name, num: row.num, tel: row.tel ?? '' })
  dialogTitle.value = '修改学院'
  dialogVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()

  // 先用列表行数据把弹窗填上（立刻可见），再拉一次详情覆盖。
  // 列表可能是几分钟前查的，别人可能已经改过；详情接口同时也验证了
  // baseinfo:college:query 这条权限是通的。
  try {
    const latest = await getCollegeInfo(id)
    Object.assign(form, { name: latest.name, num: latest.num, tel: latest.tel ?? '' })
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
    const payload: CollegeForm = {
      id: form.id,
      name: form.name.trim(),
      num: form.num.trim(),
      tel: form.tel.trim(),
    }
    // 新增和修改共用后端 /save 一个接口，靠 payload.id 是否为 null 区分，
    // 后端也是按这个判该校验 add 还是 update 权限。
    // 提示文案仍然分两种：接口返回的 msg 是「新增成功/修改成功」，但被
    // request.ts 剥掉了（拦截器只把 data 交出来），所以这里自己写。
    const isAdd = payload.id == null
    await saveCollege(payload)
    ElMessage.success(isAdd ? '新增成功' : '修改成功')
    dialogVisible.value = false
    // 新增的记录排在表的末尾，当前页不一定看得见，重新按当前条件查一次即可；
    // 用 load 而不是 search，是为了不把用户翻到的页码踢回第一页。
    await page.load()
    await refreshCollegeDict()
  } catch {
    // 重名（50201）、无权限（403）等业务错误，拦截器已经弹过提示，这里不重复弹
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (row: College) => {
  const id = row.id
  if (id == null) return

  try {
    await ElMessageBox.confirm(`确定删除学院「${row.name}」吗？删除后无法恢复。`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return // 用户点了取消
  }

  try {
    await deleteCollege(id)
    ElMessage.success('删除成功')
    // 删的正好是当前页最后一条时会自动往前退一页，不会停在空页上
    await page.reloadAfterDelete()
    await refreshCollegeDict()
  } catch {
    // 该学院下还有班级 / 教师时后端会拒绝（50203 / 50204），提示由拦截器弹出
  }
}

/**
 * 学院字典被别的页面复用（班级管理、教师管理都要选所属学院），
 * 这里增删改完顺手局部刷新，不用等用户重新登录或整页刷新。
 * 失败不影响主流程，所以吞掉异常。
 */
const refreshCollegeDict = async () => {
  try {
    await baseInfoStore.reload(['colleges'])
  } catch {
    // 字典刷新失败不该让用户以为保存失败了
  }
}
</script>

<template>
  <div class="college-page">
    <el-card shadow="never">
      <template #header>
        <span class="card-title">学院管理</span>
      </template>

      <!-- 查询栏 -->
      <el-form :inline="true" class="search-bar" @submit.prevent>
        <el-form-item label="学院名称">
          <el-input v-model="page.form.name" placeholder="支持模糊查询" clearable style="width: 180px"
            @keyup.enter="page.search" @clear="page.search" />
        </el-form-item>
        <el-form-item label="学院编号">
          <el-input v-model="page.form.num" placeholder="支持模糊查询" clearable style="width: 150px"
            @keyup.enter="page.search" @clear="page.search" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="page.search">查询</el-button>
          <el-button :icon="Refresh" @click="page.reset">重置</el-button>
          <el-button v-if="loginStore.hasPermission(PERM_ADD)" type="success" :icon="Plus" @click="openAdd">
            新增学院
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 列表 -->
      <el-table :data="page.records" v-loading="page.loading" border stripe>
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="name" label="学院名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="num" label="学院编号" width="120" align="center" />
        <el-table-column label="办公电话" width="170" align="center">
          <template #default="{ row }">{{ row.tel || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default="{ row }">
            <el-button v-if="loginStore.hasPermission(PERM_UPDATE)" link type="primary" @click="openEdit(row)">
              修改
            </el-button>
            <el-button v-if="loginStore.hasPermission(PERM_DELETE)" link type="danger" @click="handleDelete(row)">
              删除
            </el-button>
            <span v-if="!loginStore.hasPermission(PERM_UPDATE) && !loginStore.hasPermission(PERM_DELETE)"
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
        <el-form-item label="学院名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入学院名称" maxlength="50" />
        </el-form-item>
        <el-form-item label="学院编号" prop="num">
          <el-input v-model="form.num" placeholder="请输入学院编号" maxlength="20" />
        </el-form-item>
        <el-form-item label="办公电话" prop="tel">
          <el-input v-model="form.tel" placeholder="如 0718-8437432，可留空" maxlength="20" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.college-page {
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
</style>
