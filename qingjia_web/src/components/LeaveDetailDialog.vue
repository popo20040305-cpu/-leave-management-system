<template>
  <el-dialog :model-value="modelValue" title="请假申请详情" width="640px" append-to-body
    @update:model-value="handleVisibleChange">
    <div v-loading="loading" class="leave-detail">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="申请人">
          {{ application?.studentName || '本人' }}
          <span v-if="application?.className" class="muted">（{{ application.className }}）</span>
        </el-descriptions-item>
        <el-descriptions-item label="当前状态">
          <el-tag :type="leaveStatusTagType(application?.status)" size="small">
            {{ application?.statusName || '-' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="请假类型">{{ application?.typeName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="请假途径">{{ application?.path || '-' }}</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ application?.begintime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ application?.endtime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="请假原因" :span="2">{{ application?.reason || '-' }}</el-descriptions-item>
        <el-descriptions-item v-if="application?.xiaoJiaTime" label="销假时间" :span="2">
          {{ application.xiaoJiaTime }}
        </el-descriptions-item>
        <el-descriptions-item v-if="application?.xiaoJiaPhotoUrl" label="销假照片" :span="2">
          <el-image :src="photoSrc(application.xiaoJiaPhotoUrl)" :preview-src-list="[photoSrc(application.xiaoJiaPhotoUrl)]"
            fit="cover" style="width: 120px; height: 120px" />
        </el-descriptions-item>
      </el-descriptions>

      <div class="timeline-title">审批记录</div>
      <el-empty v-if="!application?.shenPiList || application.shenPiList.length === 0" description="还没有审批记录"
        :image-size="60" />
      <el-timeline v-else>
        <el-timeline-item v-for="(item, index) in application.shenPiList" :key="item.id ?? index"
          :type="item.result === 1 ? 'success' : 'danger'" :timestamp="item.createTime || ''">
          <div class="timeline-item">
            <span class="timeline-title-text">
              {{ leaveRoleText(item.roleType) }} {{ item.teacherName || '' }}
              <el-tag :type="item.result === 1 ? 'success' : 'danger'" size="small" class="result-tag">
                {{ leaveResultText(item.result) }}
              </el-tag>
            </span>
            <div class="timeline-opinion">{{ item.opinion || '（无审批意见）' }}</div>
          </div>
        </el-timeline-item>
      </el-timeline>
    </div>

    <template #footer>
      <slot name="footer">
        <el-button @click="handleVisibleChange(false)">关闭</el-button>
      </slot>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
/**
 * 请假申请详情弹窗（四个页面共用）。
 *
 * 故意做成「哑组件」：只负责渲染，详情数据由父组件拉好传进来。
 * 因为四个页面拿详情的接口不同（/api/leave/student|banzhuren|fudaoyuan|yuanlingdao/detail），
 * 弹窗自己去判断该调哪个反而更难维护；父组件通常还要拿这份数据做别的事。
 */
import type { LeaveApplicationVo } from '@/type/leavePojo'
import { leaveResultText, leaveRoleText, leaveStatusTagType } from '@/type/leavePojo'
import { BASE_URL } from '@/utils/config'

defineProps<{
  modelValue: boolean
  /** 详情数据；没拉到详情时可以先传列表行 */
  application: LeaveApplicationVo | null
  loading?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

const handleVisibleChange = (value: boolean) => {
  emit('update:modelValue', value)
}

/** 销假照片存的是 /xiaojiaPhoto/xxx.jpg，静态资源挂在 context-path 下 */
const photoSrc = (url?: string | null) => (url ? `${BASE_URL}${url}` : '')
</script>

<style scoped>
.leave-detail {
  min-height: 120px;
}

.muted {
  color: #909399;
}

.timeline-title {
  margin: 18px 0 12px;
  font-weight: 600;
  color: #303133;
}

.timeline-title-text {
  font-weight: 600;
}

.result-tag {
  margin-left: 8px;
}

.timeline-opinion {
  margin-top: 4px;
  color: #606266;
}
</style>
