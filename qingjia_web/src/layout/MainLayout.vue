<template>
    <div class="app-wrapper">
        <!-- 顶部 Header -->
        <header class="header">
            <div class="logo">后台管理系统</div>
            <div class="user-info">
                <!-- 鼠标悬停用户名弹出下拉框：修改密码 / 注销 -->
                <el-dropdown trigger="hover" placement="bottom-end" @command="handleCommand">
                    <span class="user-trigger">
                        <el-avatar :size="28" class="avatar">{{ avatarText }}</el-avatar>
                        <span class="username">{{ store.loginUserName }}</span>
                        <el-icon class="caret">
                            <ArrowDown />
                        </el-icon>
                    </span>
                    <template #dropdown>
                        <el-dropdown-menu>
                            <el-dropdown-item command="password">
                                <el-icon>
                                    <Lock />
                                </el-icon>
                                修改密码
                            </el-dropdown-item>
                            <el-dropdown-item command="logout" divided>
                                <el-icon>
                                    <SwitchButton />
                                </el-icon>
                                注销
                            </el-dropdown-item>
                        </el-dropdown-menu>
                    </template>
                </el-dropdown>
            </div>
        </header>

        <!-- 主体部分（左侧菜单 + 右侧内容） -->
        <div class="main-body">
            <aside class="sidebar">
                <SidebarMenu />
            </aside>

            <section class="content">
                <router-view v-slot="{ Component }">
                    <keep-alive>
                        <component :is="Component" />
                    </keep-alive>
                </router-view>
            </section>
        </div>

        <!-- 修改密码弹窗 -->
        <ChangePasswordDialog v-model="passwordDialogVisible" @success="handlePasswordChanged" />
    </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { ArrowDown, Lock, SwitchButton } from '@element-plus/icons-vue'
import SidebarMenu from '../components/SidebarMenu.vue'
import ChangePasswordDialog from '../components/ChangePasswordDialog.vue'
import { useLoginStore } from '@/stores/loginStores'
import { useMenuStore } from '@/stores/menuStore'
import { useBaseInfoStore } from '@/stores/baseInfoStore'

const store = useLoginStore()
const menuStore = useMenuStore()
const baseInfoStore = useBaseInfoStore()
const router = useRouter()

const passwordDialogVisible = ref(false)

const avatarText = computed(() => {
    const name = store.loginUserName
    return name ? name.charAt(0) : '?'
})

const handleCommand = (command: string | number | object) => {
    if (command === 'password') {
        passwordDialogVisible.value = true
    } else if (command === 'logout') {
        confirmLogout()
    }
}

const confirmLogout = async () => {
    try {
        await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning',
        })
    } catch {
        return // 用户点了取消
    }
    // store.logout() 会先调后端作废 token，再清 localStorage 和 store
    await store.logout()
    // 菜单和动态路由也要清，否则换账号登录会看到上一个账号的菜单
    menuStore.reset()
    // 学院字典后端是按登录用户过滤的，换账号必须重取
    baseInfoStore.reset()
    await router.push('/login')
}

// 改密成功后后端已作废 token、store 也已清本地状态，这里清掉会话级缓存并回登录页
const handlePasswordChanged = async () => {
    menuStore.reset()
    baseInfoStore.reset()
    await router.push('/login')
}
</script>

<style scoped>
/* 注意：scoped 样式里的 * 只作用于本组件模板内的元素，
   碰不到 html/body。真正的全局重置在 src/assets/main.css 里。 */
* {
    margin: 0;
    padding: 0;
    box-sizing: border-box;
}

.app-wrapper {
    height: 100vh;
    display: flex;
    flex-direction: column;
    font-family: 'Segoe UI', sans-serif;
}

/* ------ 顶部导航栏 ------ */
.header {
    height: 60px;
    background: #2c3e50;
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 24px;
    flex-shrink: 0;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    z-index: 10;
}

.logo {
    font-size: 20px;
    font-weight: bold;
    letter-spacing: 1px;
}

.user-info {
    display: flex;
    align-items: center;
    gap: 16px;
    font-size: 14px;
}

.username {
    opacity: 0.9;
}

/* 下拉框触发区：el-dropdown 默认字色是深色，在深色 header 上必须显式改成白色 */
.user-trigger {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 4px 10px;
    border-radius: 4px;
    color: #fff;
    cursor: pointer;
    outline: none;
    transition: background-color 0.2s;
}

.user-trigger:hover {
    background: rgba(255, 255, 255, 0.12);
}

.avatar {
    flex-shrink: 0;
    background-color: #1abc9c;
    color: #fff;
    font-size: 13px;
}

.caret {
    font-size: 12px;
    opacity: 0.75;
}

/* ------ 主体：左菜单 + 右内容 ------ */
/* 这三条原来整个缺失，导致 .main-body 退化成普通块级 div，
   aside 和 section 上下堆叠 —— 菜单跑到了页面顶部。 */
.main-body {
    flex: 1;
    display: flex;
    /* min-height:0 是关键。flex 子项默认 min-height:auto，
       不加的话内部内容会把 100vh 的容器撑破，滚动条落到 body 上。 */
    min-height: 0;
    overflow: hidden;
}

.sidebar {
    width: 220px;
    flex-shrink: 0;
    background-color: #34495e;
    overflow-y: auto;
    overflow-x: hidden;
}

.content {
    flex: 1;
    /* min-width:0 同理，否则内容区的宽表格会把 flex 布局撑破 */
    min-width: 0;
    overflow-y: auto;
    padding: 16px;
    background-color: #f0f2f5;
}
</style>
