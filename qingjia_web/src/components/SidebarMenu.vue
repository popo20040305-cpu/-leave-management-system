<template>
    <el-menu :default-active="activeMenu" background-color="#34495e" text-color="#ecf0f1" active-text-color="#1abc9c"
        router unique-opened>
        <template v-for="item in menuStore.menuVos" :key="item.id">
            <el-sub-menu v-if="item.type === 1 && item.subMenus && item.subMenus.length" :index="String(item.id)">
                <template #title>
                    <el-icon>
                        <component :is="getIcon(item.icon)" />
                    </el-icon>
                    <span>{{ item.name }}</span>
                </template>

                <!-- 子菜单项 -->
                <el-menu-item v-for="child in item.subMenus" :key="child.id" :index="getRoutePath(child.url)">
                    <el-icon>
                        <component :is="getIcon(child.icon) || 'Menu'" />
                    </el-icon>
                    <span>{{ child.name }}</span>
                </el-menu-item>
            </el-sub-menu>
        </template>
    </el-menu>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useMenuStore } from '@/stores/menuStore'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import type { Component } from 'vue'

const menuStore = useMenuStore()
const route = useRoute()

const activeMenu = computed(() => route.path)

const getRoutePath = (url: string | null) => {
    if (!url) return ''
    return url.split('?')[0]
}

const getIcon = (icon: string | null) => {
    if (!icon) return 'Menu'
    const name = icon.startsWith('el-icon-') ? icon.slice(8) : icon
    const icons = ElementPlusIconsVue as Record<string, Component>
    return icons[name] || 'Menu'
}
</script>

<style scoped>
:deep(.el-menu) {
    border-right: none;
}

:deep(.el-sub-menu .el-menu-item) {
    padding-left: 50px !important;
}
</style>