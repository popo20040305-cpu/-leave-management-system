<script setup lang="ts">
/**
 * 院领导审批（菜单「请假管理 -> 院领导审批」，url = /leave/yuanlingdao/index）。
 *
 * 数据范围与辅导员一致（本学院 + 年级/班级筛选），差别是后端额外要求
 * 「学院领导」角色（@RequiresRoles）。
 */
import LeaveApprovalPanel from '@/components/LeaveApprovalPanel.vue'
</script>

<template>
  <LeaveApprovalPanel role="yuanlingdao" title="院领导审批" />
</template>
