<template>
  <div class="leave-approval">
    <el-card shadow="never">
      <template #header>
        <span class="card-title">{{ title }}</span>
      </template>

      <el-form :inline="true" class="search-bar" @submit.prevent>
        <el-form-item v-if="showGrade" label="年级">
          <el-select v-model="page.form.grade" placeholder="全部" clearable style="width: 130px">
            <el-option v-for="grade in baseInfoStore.grades" :key="grade.id" :label="`${grade.grade} 级`"
              :value="grade.grade" />
          </el-select>
        </el-form-item>
        <el-form-item label="班级">
          <el-select v-model="page.form.classId" placeholder="全部" clearable filterable style="width: 160px">
            <el-option v-for="clazz in classes" :key="clazz.id" :label="clazz.num" :value="clazz.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="page.form.status" placeholder="全部" clearable style="width: 170px">
            <el-option v-for="status in baseInfoStore.applicationStatuses" :key="status.id"
              :label="status.applicationStutas" :value="status.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="page.search">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
          <span class="tip">默认只看「{{ pendingStatusText }}」的申请</span>
        </el-form-item>
      </el-form>

      <el-table :data="page.records" v-loading="page.loading" border stripe>
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column label="学生" width="110" show-overflow-tooltip>
          <template #default="{ row }">{{ row.studentName || '-' }}</template>
        </el-table-column>
        <el-table-column label="班级" width="120" align="center">
          <template #default="{ row }">{{ row.className || '-' }}</template>
        </el-table-column>
        <el-table-column label="请假类型" width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.typeName || '-' }}</template>
        </el-table-column>
        <el-table-column label="请假时间" min-width="290">
          <template #default="{ row }">{{ row.begintime || '-' }} ~ {{ row.endtime || '-' }}</template>
        </el-table-column>
        <el-table-column label="请假原因" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.reason || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="140" align="center">
          <template #default="{ row }">
            <el-tag :type="leaveStatusTagType(row.status)" size="small">{{ row.statusName || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="info" @click="openDetail(row)">详情</el-button>
            <el-button v-if="canApprove(row)" link type="primary" @click="openApprove(row)">审批</el-button>
          </template>
        </el-table-column>
        <template #empty>暂无数据</template>
      </el-table>

      <Pagination v-model:current="page.current" v-model:size="page.size" :total="page.total"
        @change="page.onPageChange" />
    </el-card>

    <LeaveDetailDialog v-model="detailVisible" :application="detail" :loading="detailLoading" />
    <LeaveApproveDialog v-model="approveVisible" :application="approveTarget" :loading="approveLoading"
      :submitting="approving" @submit="handleApproveSubmit" />
  </div>
</template>

<script setup lang="ts">
/**
 * 三级审批的公共面板（班主任 / 辅导员 / 院领导三个页面共用）。
 *
 * 三个页面的差别只有三处：审批接口的 URL 段（role）、要不要年级筛选、
 * 「等我批」的状态值。全部用 props + LEAVE_PENDING_STATUS 表达，
 * 所以 views/leave/{banzhuren,fudaoyuan,yuanlingdao}/ 下只剩一层薄包装。
 */
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import Pagination from '@/components/Pagination.vue'
import LeaveDetailDialog from '@/components/LeaveDetailDialog.vue'
import LeaveApproveDialog from '@/components/LeaveApproveDialog.vue'
import { usePageTable } from '@/composables/usePageTable'
import { useBaseInfoStore } from '@/stores/baseInfoStore'
import {
  approveLeave,
  getLeaveApprovalClasses,
  getLeaveApprovalDetail,
  getLeaveApprovalList,
  type LeaveApprovalRole,
} from '@/api/leaveApi'
import {
  LEAVE_PENDING_STATUS,
  leaveStatusTagType,
  type LeaveApplicationVo,
} from '@/type/leavePojo'
import type { ClassPojo } from '@/type/classPojo'

const props = defineProps<{
  /** 审批端角色，决定接口 URL 段 */
  role: LeaveApprovalRole
  /** 页面标题 */
  title: string
}>()

const baseInfoStore = useBaseInfoStore()

/** 只有辅导员 / 院领导需要按年级筛（班主任的范围就是自己带的几个班） */
const showGrade = computed(() => props.role !== 'banzhuren')

const page = usePageTable<LeaveApplicationVo, { status?: number; grade?: number; classId?: number }>(
  (params) => getLeaveApprovalList(props.role, params),
  // 默认只看「等我批的」：审批页面打开就该是待办，而不是全部历史
  { query: { status: LEAVE_PENDING_STATUS[props.role], grade: undefined, classId: undefined } },
)

const pendingStatusText = computed(
  () => baseInfoStore.labelOf('applicationStatuses', LEAVE_PENDING_STATUS[props.role]) || '待审批',
)

/* ---------------- 班级下拉 ---------------- */

const classes = ref<ClassPojo[]>([])

const loadClasses = async () => {
  try {
    classes.value = await getLeaveApprovalClasses(
      props.role,
      showGrade.value ? page.form.grade : undefined,
    )
  } catch {
    classes.value = []
  }
}

// 年级变了要重新取班级，并且把已经选中的班级清掉（它可能不在新范围里了）
watch(
  () => page.form.grade,
  () => {
    page.form.classId = undefined
    void loadClasses()
  },
)

onMounted(() => {
  void loadClasses()
})

/** 重置：状态回到「等我批的」，年级/班级清空 */
const handleReset = () => {
  page.form.grade = undefined
  page.form.classId = undefined
  page.form.status = LEAVE_PENDING_STATUS[props.role]
  void page.search()
  void loadClasses()
}

/* ---------------- 详情 / 审批 ---------------- */

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
    detail.value = await getLeaveApprovalDetail(props.role, id)
  } catch {
    // 拉不到就沿用列表行，提示由拦截器弹出
  } finally {
    detailLoading.value = false
  }
}

const approveVisible = ref(false)
const approving = ref(false)
const approveLoading = ref(false)
const approveTarget = ref<LeaveApplicationVo | null>(null)

/** 只有「等我批」的那一条才显示审批按钮，避免对已归档/已驳回的申请重复审批 */
const canApprove = (row: LeaveApplicationVo) => row.status === LEAVE_PENDING_STATUS[props.role]

const openApprove = async (row: LeaveApplicationVo) => {
  const id = row.id
  if (id == null) return
  approveTarget.value = row
  approveVisible.value = true
  approveLoading.value = true
  try {
    approveTarget.value = await getLeaveApprovalDetail(props.role, id)
  } catch {
    // 拉不到就沿用列表行
  } finally {
    approveLoading.value = false
  }
}

const handleApproveSubmit = async (payload: { result: number; opinion: string }) => {
  const id = approveTarget.value?.id
  if (id == null) return
  approving.value = true
  try {
    await approveLeave(props.role, {
      applicationId: id,
      result: payload.result,
      opinion: payload.opinion,
    })
    ElMessage.success('审批成功')
    approveVisible.value = false
    // 审批后这条申请通常已经不在当前筛选条件（待审批）里了，重新拉一次列表
    await page.load()
  } catch {
    // 状态已被别人处理（51003）等由拦截器提示
  } finally {
    approving.value = false
  }
}
</script>

<style scoped>
.leave-approval {
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

.tip {
  margin-left: 8px;
  color: #909399;
  font-size: 12px;
}
</style>
