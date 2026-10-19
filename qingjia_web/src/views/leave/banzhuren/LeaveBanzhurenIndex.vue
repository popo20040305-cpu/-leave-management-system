<script setup lang="ts">
/**
 * 班主任审批（菜单「请假管理 -> 班主任审批」，url = /leave/banzhuren/index）。
 *
 * 只做一层包装：真正的筛选、列表、详情、审批都在 LeaveApprovalPanel 里，
 * 三个审批页面（班主任/辅导员/院领导）共用同一份逻辑。
 */
import LeaveApprovalPanel from '@/components/LeaveApprovalPanel.vue'
</script>

<template>
  <LeaveApprovalPanel role="banzhuren" title="班主任审批" />
</template>
