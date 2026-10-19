<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useLoginStore } from '@/stores/loginStores'
import { useMenuStore } from '@/stores/menuStore'
import { useBaseInfoStore } from '@/stores/baseInfoStore'

const router = useRouter()
const loginStore = useLoginStore()
const menuStore = useMenuStore()
const baseInfoStore = useBaseInfoStore()

const userTypeText = computed(() => {
  if (loginStore.userInfo?.type === 1) return '学生'
  if (loginStore.userInfo?.type === 2) return '教师'
  return '用户'
})

const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return // 用户取消
  }

  // store.logout() 内部已经会调后端 /api/logout，且失败也照样清本地登录态。
  // 原来这里自己又调了一次 logoutApi()，同一个登出请求会打两遍。
  try {
    await loginStore.logout()
  } catch {
    loginStore.clearLocalState()
  }
  // 菜单/动态路由/基础数据都是会话级缓存，换账号之前必须清掉
  menuStore.reset()
  baseInfoStore.reset()
  ElMessage.success('已退出登录')
  router.replace('/login')
}
</script>

<template>
  <el-container class="home-container">
    <el-header class="home-header">
      <span class="home-title">请假管理系统</span>
      <span class="home-user">
        {{ loginStore.loginUserName }}（{{ userTypeText }}）
        <el-button type="danger" link @click="handleLogout">退出登录</el-button>
      </span>
    </el-header>
    <el-main class="home-main">
      <el-card shadow="never">
        <el-empty description="登录联调已打通，业务页面正在建设中" />
      </el-card>
    </el-main>
  </el-container>
</template>

<style scoped>
.home-container {
  height: 100vh;
}

.home-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #001529;
  color: #fff;
}

.home-title {
  font-size: 18px;
  font-weight: 600;
}

.home-user {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  color: rgba(255, 255, 255, 0.85);
}

.home-main {
  background: #f0f2f5;
}
</style>
