// utils/routeHelper.ts

import type { MenuVo } from '@/type/menu'
import type { RouteRecordRaw } from 'vue-router'

/**
 * 递归收集所有叶子菜单（有 url 的项）
 */
function collectLeafMenus(menus: MenuVo[]): MenuVo[] {
  const leaves: MenuVo[] = []

  for (const menu of menus) {
    if (menu.url) {
      leaves.push(menu)
    }
    if (menu.subMenus && menu.subMenus.length) {
      leaves.push(...collectLeafMenus(menu.subMenus))
    }
  }

  return leaves
}

/**
 * 将 url 转换为组件路径与组件名
 *
 * 规则：
 * - 去掉查询参数（？之后的内容）
 * - 按 '/' 分割路径，每个分段首字母大写并拼接，作为文件名
 * - 目录为除去最后一部分的路径（多个目录以 '/' 连接）
 *
 * 示例：
 * /college/index    -> dir: 'college', fileName: 'CollegeIndex.vue'
 * /leave/student/index -> dir: 'leave/student', fileName: 'LeaveStudentIndex.vue'
 * /system/user      -> dir: 'system', fileName: 'SystemUser.vue'
 */
function urlToComponentPath(url: string): { dir: string; fileName: string } {
  // 1. 去除查询参数，并确保结果不为 undefined
  const path = url.split('?')[0]!

  // 2. 去除首尾斜杠，按 '/' 分割
  const parts = path.replace(/^\/+|\/+$/g, '').split('/')

  if (parts.length === 0) {
    throw new Error(`Invalid url: ${url}`)
  }

  // 3. 调试日志（可移除）
  console.log('[urlToComponentPath] 输入 url:', url)
  console.log('[urlToComponentPath] 分割后 parts:', parts)

  // 4. 生成文件名：所有分段首字母大写并拼接
  const fileName =
    parts.map((part) => part.charAt(0).toUpperCase() + part.slice(1)).join('') + '.vue'

  // 5. 目录路径：除去最后一部分
  const dir = parts.slice(0, -1).join('/')

  console.log('[urlToComponentPath] 生成 dir:', dir, 'fileName:', fileName)

  return { dir, fileName }
}

/**
 * 根据菜单列表生成动态路由（挂载到 '/' 的 children）
 */
export function generateDynamicRoutes(menuVos: MenuVo[]): RouteRecordRaw[] {
  const leaves = collectLeafMenus(menuVos)

  const modules = import.meta.glob('@/views/**/*.vue') // 得到实际实现的组件

  // 调试：打印所有可用的模块键
  console.log('【debug】所有 modules 键:', Object.keys(modules))

  const routes: RouteRecordRaw[] = []

  for (const leaf of leaves) {
    const url = leaf.url
    if (!url) continue

    // 确保路径以 '/' 开头
    let path = url.split('?')[0]!
    if (!path.startsWith('/')) {
      path = '/' + path
    }

    const { dir, fileName } = urlToComponentPath(url)

    // 构造可能的完整路径格式
    const targetPath = `${dir}/${fileName}` // 例如 "college/CollegeIndex.vue"

    // 在 modules 中查找键
    let component = null

    // 方式1：尝试几种常见前缀
    const prefixes = ['@/views/', '/src/views/', 'src/views/', '../views/']

    for (const prefix of prefixes) {
      const fullKey = prefix + targetPath
      if (modules[fullKey]) {
        component = modules[fullKey]
        break
      }
    }

    // 方式2：如果还没找到，遍历所有键，进行结尾匹配（忽略大小写？但精确匹配更安全）
    if (!component) {
      const matchedKey = Object.keys(modules).find((key) => key.endsWith(targetPath))
      if (matchedKey) {
        component = modules[matchedKey]
      }
    }

    if (!component) {
      console.warn(`组件 ${targetPath} 不存在，跳过路由 ${path}`)
      continue
    }

    routes.push({
      path,
      name: leaf.name || path.replace(/^\//, '').replace(/\//g, '_'),
      component,
      meta: {
        title: leaf.name,
        icon: leaf.icon,
        permission: leaf.permission,
      },
    })
  }

  console.log('生成的动态路由:', routes)

  return routes
}
