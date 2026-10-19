<script setup lang="ts">
/**
 * 辅导员审批（菜单「请假管理 -> 辅导员审批」，url = /leave/fudaoyuan/index）。
 *
 * 与班主任审批的区别：数据范围是自己所在学院（可按年级 / 班级筛），
 * 而不是自己带的班 —— 这个差别在 LeaveApprovalPanel 里由 role 决定。
 */
import LeaveApprovalPanel from '@/components/LeaveApprovalPanel.vue'
</script>

<template>
  <LeaveApprovalPanel role="fudaoyuan" title="辅导员审批" />
</template>
