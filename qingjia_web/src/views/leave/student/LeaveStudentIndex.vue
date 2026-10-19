<script setup lang="ts">
/**
 * 我的申请（菜单「请假管理 -> 我的申请」，url = /leave/student/index）。
 *
 * 对着老 Thymeleaf 版 templates/leave/myLeave.html 搬的：
 * 列表 + 状态筛选、填表（存草稿 / 直接提交）、提交草稿、删除草稿、详情、销假。
 *
 * 按钮可见性完全跟着「状态 + 权限」走，因为后端对状态卡得很死：
 *   1 待提交  -> 可以修改 / 提交 / 删除
 *   2~4 审批中 -> 只能看详情
 *   5 待销假  -> 可以上传照片销假（后端 xiaoJia 会校验状态，非 5 直接报 51003）
 *   6 未通过 / 7 归档 -> 只能看详情
 * 所以「修改」这类按钮在非草稿行上根本不该出现，而不是点下去才报错。
 */
import { computed, nextTick, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules, UploadFile } from 'element-plus'
import { Plus, Refresh, Search, Upload } from '@element-plus/icons-vue'
import Pagination from '@/components/Pagination.vue'
import LeaveDetailDialog from '@/components/LeaveDetailDialog.vue'
import { usePageTable } from '@/composables/usePageTable'
import { useLoginStore } from '@/stores/loginStores'
import { useBaseInfoStore } from '@/stores/baseInfoStore'
import {
  deleteLeaveDraft,
  getMyLeaveDetail,
  getMyLeaveList,
  saveLeaveDraft,
  submitLeave,
  submitLeaveDraft,
  xiaoJiaLeave,
} from '@/api/leaveApi'
import {
  emptyLeaveForm,
  LEAVE_STATUS,
  leaveStatusTagType,
  type LeaveApplicationVo,
  type LeaveForm,
} from '@/type/leavePojo'

const loginStore = useLoginStore()
const baseInfoStore = useBaseInfoStore()

const PERM_ADD = 'leave:student:add'
const PERM_DETAIL = 'leave:student:detail'
const PERM_DELETE = 'leave:student:delete'
const PERM_XIAOJIA = 'leave:student:xiaojia'

const page = usePageTable<LeaveApplicationVo, { status?: number }>(
  (params) => getMyLeaveList(params),
  { query: { status: undefined } },
)

/* ---------------- 新增 / 修改草稿弹窗 ---------------- */

const dialogVisible = ref(false)
const dialogTitle = ref('新增请假申请')
const submitting = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<LeaveForm>(emptyLeaveForm())

const rules: FormRules = {
  type: [{ required: true, message: '请选择请假类型', trigger: 'change' }],
  path: [
    { required: true, message: '请输入请假途径', trigger: 'blur' },
    // 后端 LeaveApplicationPojo.path 上是 @Size(max = 50)
    { max: 50, message: '请假途径不超过 50 个字符', trigger: 'blur' },
  ],
  reason: [
    { required: true, message: '请输入请假原因', trigger: 'blur' },
    { max: 200, message: '请假原因不超过 200 个字符', trigger: 'blur' },
  ],
  begintime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endtime: [
    { required: true, message: '请选择结束时间', trigger: 'change' },
    {
      // 后端也有一条 @AssertTrue 的「结束时间必须晚于开始时间」，
      // 在表单里先挡一次，避免提交完才看到校验错误
      validator: (_rule: unknown, value: string, callback: (e?: Error) => void) => {
        if (!value || !form.begintime) return callback()
        return new Date(value).getTime() > new Date(form.begintime).getTime()
          ? callback()
          : callback(new Error('结束时间必须晚于开始时间'))
      },
      trigger: 'change',
    },
  ],
}

const openAdd = async () => {
  Object.assign(form, emptyLeaveForm())
  dialogTitle.value = '新增请假申请'
  dialogVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}

const openEdit = async (row: LeaveApplicationVo) => {
  const id = row.id
  if (id == null) return
  Object.assign(form, {
    id,
    type: row.type ?? null,
    path: row.path ?? '',
    reason: row.reason ?? '',
    begintime: row.begintime ?? '',
    endtime: row.endtime ?? '',
  })
  dialogTitle.value = '修改请假申请'
  dialogVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}

/**
 * 提交表单。mode='draft' 存草稿（状态 1），'submit' 直接提交（状态 2 待班主任审批）。
 */
const handleSubmit = async (mode: 'draft' | 'submit') => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const payload: LeaveForm = {
      id: form.id,
      type: form.type,
      path: form.path.trim(),
      reason: form.reason.trim(),
      begintime: form.begintime,
      endtime: form.endtime,
    }
    if (mode === 'draft') {
      await saveLeaveDraft(payload)
      ElMessage.success('草稿保存成功')
    } else {
      await submitLeave(payload)
      ElMessage.success('提交成功，等待班主任审批')
    }
    dialogVisible.value = false
    await page.load()
  } catch {
    // 状态已变（51003）、不属于本人（51001）等由拦截器提示
  } finally {
    submitting.value = false
  }
}

const handleSubmitDraft = async (row: LeaveApplicationVo) => {
  const id = row.id
  if (id == null) return
  try {
    await ElMessageBox.confirm('提交后进入班主任审批，期间不能修改，确定提交吗？', '提示', {
      confirmButtonText: '提交',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return // 用户取消
  }

  try {
    await submitLeaveDraft(id)
    ElMessage.success('提交成功，等待班主任审批')
    await page.load()
  } catch {
    // 只有待提交状态能提交，否则后端报 51002/51003
  }
}

const handleDelete = async (row: LeaveApplicationVo) => {
  const id = row.id
  if (id == null) return
  try {
    await ElMessageBox.confirm('确定删除这条草稿吗？删除后无法恢复。', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return // 用户取消
  }

  try {
    await deleteLeaveDraft(id)
    ElMessage.success('删除成功')
    await page.reloadAfterDelete()
  } catch {
    // 非草稿不能删，后端返回错误
  }
}

/* ---------------- 详情 ---------------- */

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<LeaveApplicationVo | null>(null)

const openDetail = async (row: LeaveApplicationVo) => {
  const id = row.id
  if (id == null) return
  detail.value = row
  detailVisible.value = true
  detailLoading.value = true
  try {
    // 列表行没有审批记录，详情才有
    detail.value = await getMyLeaveDetail(id)
  } catch {
    // 拉不到就沿用列表行
  } finally {
    detailLoading.value = false
  }
}

/* ---------------- 销假 ---------------- */

const xiaoJiaVisible = ref(false)
const xiaoJiaSubmitting = ref(false)
const xiaoJiaTarget = ref<LeaveApplicationVo | null>(null)
const xiaoJiaFile = ref<File | null>(null)

const openXiaoJia = (row: LeaveApplicationVo) => {
  xiaoJiaTarget.value = row
  xiaoJiaFile.value = null
  xiaoJiaVisible.value = true
}

// el-upload 用 :auto-upload="false"，文件先留在前端，等用户点确定再和 applicationId 一起提交
const handleFileChange = (file: UploadFile) => {
  const raw = file.raw
  if (!raw) return
  if (!raw.type.startsWith('image/')) {
    ElMessage.warning('销假凭证必须是图片')
    xiaoJiaFile.value = null
    return
  }
  if (raw.size > 5 * 1024 * 1024) {
    ElMessage.warning('图片不能超过 5MB')
    xiaoJiaFile.value = null
    return
  }
  xiaoJiaFile.value = raw
}

const handleXiaoJia = async () => {
  const id = xiaoJiaTarget.value?.id
  if (id == null) return
  if (!xiaoJiaFile.value) {
    ElMessage.warning('请先选择销假照片')
    return
  }
  xiaoJiaSubmitting.value = true
  try {
    await xiaoJiaLeave(id, xiaoJiaFile.value)
    ElMessage.success('销假成功，申请已归档')
    xiaoJiaVisible.value = false
    await page.load()
  } catch {
    // 状态不是「待销假」（51003）、没传照片（51008）等由拦截器提示
  } finally {
    xiaoJiaSubmitting.value = false
  }
}

/* ---------------- 按钮可见性 ---------------- */
const isDraft = (row: LeaveApplicationVo) => row.status === LEAVE_STATUS.DRAFT
const canEdit = computed(() => loginStore.hasPermission(PERM_ADD))
const canDelete = computed(() => loginStore.hasPermission(PERM_DELETE))
const canDetail = computed(() => loginStore.hasPermission(PERM_DETAIL))
const canXiaoJia = computed(() => loginStore.hasPermission(PERM_XIAOJIA))
</script>

<template>
  <div class="leave-student">
    <el-card shadow="never">
      <template #header>
        <span class="card-title">我的申请</span>
      </template>

      <el-form :inline="true" class="search-bar" @submit.prevent>
        <el-form-item label="状态">
          <el-select v-model="page.form.status" placeholder="全部" clearable style="width: 180px">
            <el-option v-for="status in baseInfoStore.applicationStatuses" :key="status.id"
              :label="status.applicationStutas" :value="status.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="page.search">查询</el-button>
          <el-button :icon="Refresh" @click="page.reset">重置</el-button>
          <el-button v-if="canEdit" type="success" :icon="Plus" @click="openAdd">新增申请</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="page.records" v-loading="page.loading" border stripe>
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column label="请假类型" width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.typeName || '-' }}</template>
        </el-table-column>
        <el-table-column label="请假途径" width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.path || '-' }}</template>
        </el-table-column>
        <el-table-column label="请假原因" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.reason || '-' }}</template>
        </el-table-column>
        <el-table-column label="开始时间" width="160" align="center">
          <template #default="{ row }">{{ row.begintime || '-' }}</template>
        </el-table-column>
        <el-table-column label="结束时间" width="160" align="center">
          <template #default="{ row }">{{ row.endtime || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="140" align="center">
          <template #default="{ row }">
            <el-tag :type="leaveStatusTagType(row.status)" size="small">{{ row.statusName || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230" align="center" fixed="right">
          <template #default="{ row }">
            <el-button v-if="canDetail" link type="info" @click="openDetail(row)">详情</el-button>
            <el-button v-if="canEdit && isDraft(row)" link type="primary" @click="openEdit(row)">修改</el-button>
            <el-button v-if="canEdit && isDraft(row)" link type="success" @click="handleSubmitDraft(row)">
              提交
            </el-button>
            <el-button v-if="canXiaoJia && row.status === LEAVE_STATUS.XIAOJIA" link type="warning"
              @click="openXiaoJia(row)">
              销假
            </el-button>
            <el-button v-if="canDelete && isDraft(row)" link type="danger" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
        <template #empty>暂无数据</template>
      </el-table>

      <Pagination v-model:current="page.current" v-model:size="page.size" :total="page.total"
        @change="page.onPageChange" />
    </el-card>

    <!-- 新增 / 修改弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="请假类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择请假类型" style="width: 100%">
            <el-option v-for="type in baseInfoStore.leaveApplicationTypes" :key="type.id" :label="type.name"
              :value="type.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="请假途径" prop="path">
          <el-input v-model="form.path" placeholder="如：学校到舞阳坝" maxlength="50" />
        </el-form-item>
        <el-form-item label="请假原因" prop="reason">
          <el-input v-model="form.reason" type="textarea" :rows="3" placeholder="请说明请假原因" maxlength="200"
            show-word-limit />
        </el-form-item>
        <el-form-item label="开始时间" prop="begintime">
          <el-date-picker v-model="form.begintime" type="datetime" placeholder="选择开始时间"
            value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束时间" prop="endtime">
          <el-date-picker v-model="form.endtime" type="datetime" placeholder="选择结束时间"
            value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button :loading="submitting" @click="handleSubmit('draft')">保存草稿</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit('submit')">直接提交</el-button>
      </template>
    </el-dialog>

    <!-- 详情 -->
    <LeaveDetailDialog v-model="detailVisible" :application="detail" :loading="detailLoading" />

    <!-- 销假 -->
    <el-dialog v-model="xiaoJiaVisible" title="销假" width="460px" :close-on-click-modal="false">
      <p class="dialog-tip">
        申请：{{ xiaoJiaTarget?.typeName }}（{{ xiaoJiaTarget?.begintime }} ~ {{ xiaoJiaTarget?.endtime }}）
      </p>
      <el-upload :auto-upload="false" :limit="1" accept="image/*" :on-change="handleFileChange"
        drag>
        <el-icon class="el-icon--upload">
          <Upload />
        </el-icon>
        <div class="el-upload__text">把销假凭证（照片）拖到这里，或<em>点击选择</em></div>
        <template #tip>
          <div class="el-upload__tip">支持图片，不超过 5MB；提交后申请进入「归档」状态</div>
        </template>
      </el-upload>
      <template #footer>
        <el-button @click="xiaoJiaVisible = false">取消</el-button>
        <el-button type="primary" :loading="xiaoJiaSubmitting" @click="handleXiaoJia">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.leave-student {
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

.dialog-tip {
  margin: 0 0 12px;
  color: #606266;
  font-size: 13px;
}
</style>
