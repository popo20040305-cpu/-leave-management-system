import { useLoginStore } from '@/stores/loginStores'

/**
 * 权限判断工具（文档 10.4 的 usePermissionUtil.ts）。
 *
 * 只是对 loginStore 里六个判断方法的一层薄封装，让页面写起来短一点：
 *   const { hasPermission } = usePermission()
 *   const canAdd = computed(() => hasPermission('system:menu:add'))
 *
 * 真正的匹配逻辑（精确字符串比对、fail-closed）在 stores/loginStores.ts 里，
 * 本文件不重复实现，避免两处规则跑偏。
 */
export function usePermission() {
  const login = useLoginStore()

  return {
    hasRole: (role: string) => login.hasRole(role),
    hasAllRole: (roles: string[]) => login.hasAllRole(...roles),
    hasAnyRole: (roles: string[]) => login.hasAnyRole(...roles),
    hasPermission: (perm: string) => login.hasPermission(perm),
    hasAllPermission: (perms: string[]) => login.hasAllPermission(...perms),
    hasAnyPermission: (perms: string[]) => login.hasAnyPermission(...perms),
  }
}
