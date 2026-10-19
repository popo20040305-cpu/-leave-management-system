import { defineStore } from 'pinia'
import {
  loginApi,
  logoutApi,
  updatePasswordApi,
  type LoginParams,
  type UpdatePasswordParams,
} from '@/api/loginApi'
import { tokenStorage, userStorage } from '@/utils/storage'
import type { LoginUserInfo } from '@/type/LoginUserInfo'

interface UserState {
  token: string | null
  userInfo: LoginUserInfo | null
}

/* ------------------------------------------------------------------ *
 * 权限判断辅助函数
 *
 * 语义对齐两处既有实现：
 * 1. 老 Thymeleaf 模板 src/main/resources/templates/common/template.html 里的
 *    hasPermission / hasAnyPermission / hasAllPermissions —— 都是精确字符串比对
 *    （userPermissions.indexOf(perm) !== -1）。
 * 2. 后端 @RequiresPermissions("baseinfo:class:query") 用的也是完整权限串。
 *
 * 所以这里一律做「精确、区分大小写」的匹配，不实现 Shiro WildcardPermission 那套
 * `*` 通配推导。理由：库里存的权限串（baseinfo:access、leave:banzhuren:access…）
 * 本身不含通配符，两种算法在当前数据上结果完全一致；而前端自己发明一套通配规则
 * 只会和后端产生分歧。真要在库里配 `baseinfo:*`，应当先让后端统一，再改这里。
 * ------------------------------------------------------------------ */

/**
 * 后端 LoginUser.roleNames / permissionNames 是 Set<String>，序列化成 JSON 数组。
 * 未登录、旧版本 localStorage 里没这两个字段、或库里存了空串/带空格的脏数据，
 * 都要在这里兜住：统一 trim 并丢掉空白项，避免 " 班主任" 这种数据让判断静默失败。
 */
function normalizeNames(raw: string[] | null | undefined): string[] {
  if (!Array.isArray(raw)) return []
  return raw
    .filter((item): item is string => typeof item === 'string')
    .map((item) => item.trim())
    .filter((item) => item.length > 0)
}

function trimName(name: unknown): string {
  return typeof name === 'string' ? name.trim() : ''
}

/**
 * 让 hasAnyRole('班主任', '辅导员') 和 hasAnyRole(['班主任', '辅导员']) 两种写法都能用，
 * 统一归一成 string[]。全是空白项时返回空数组，由调用方按「不通过」处理。
 */
function toNameList(args: Array<string | string[] | null | undefined>): string[] {
  const result: string[] = []
  for (const arg of args) {
    if (arg == null) continue
    if (Array.isArray(arg)) {
      for (const item of arg) {
        const name = trimName(item)
        if (name) result.push(name)
      }
    } else {
      const name = trimName(arg)
      if (name) result.push(name)
    }
  }
  return result
}

export const useLoginStore = defineStore('login', {
  state: (): UserState => ({
    token: tokenStorage.getToken() || null,
    userInfo: userStorage.get<LoginUserInfo>() || null,
  }),

  actions: {
    // 登录
    async login(params: LoginParams) {
      const data = await loginApi(params)

      // 假设后端返回token
      const token = data.token
      if (!token) {
        throw new Error('登录接口未返回 token')
      }

      // 保存 token
      tokenStorage.setToken(token)
      this.token = token

      // 保存用户信息
      const userInfo = data.loginUserInfo as unknown as LoginUserInfo
      this.userInfo = userInfo
      userStorage.set(userInfo)

      return data
    },

    /**
     * 登出：先通知后端删掉 user_token 记录，再清本地状态。
     * 后端调不通（token 已过期、断网）也必须清本地，
     * 否则用户会卡在“点了注销没反应”。
     */
    async logout() {
      try {
        await logoutApi()
      } catch {
        // 忽略：本地状态照样要清
      }
      this.clearLocalState()
    },

    /**
     * 修改密码。后端成功后已作废当前 token，
     * 所以这里直接清本地登录态，跳转登录页由调用方负责。
     */
    async changePassword(params: UpdatePasswordParams) {
      await updatePasswordApi(params)
      this.clearLocalState()
    },

    // 只清本地（localStorage + store），不发请求
    clearLocalState() {
      tokenStorage.removeToken()
      userStorage.remove()

      this.token = null
      this.userInfo = null
    },

    // 重置 token
    resetToken() {
      tokenStorage.removeToken()
      this.token = null
    },

    /* ---------------- 权限判断（10.3.1） ---------------- *
     *
     * 六个方法都读 getters 里的 roles / permissions，而这两个 getter 依赖
     * state.userInfo，所以在模板 v-if 里直接调用是有响应式的：登录后 userInfo
     * 一变，按钮会自己显隐，不用手动刷新。
     *
     * 共同的边界约定：
     * - 未登录 / userInfo 里没有这两个字段 → 全部返回 false；
     * - 参数为空（没传、传空数组、传空白串）→ 全部返回 false。
     *   这一条是刻意的：权限判断必须 fail-closed。若按数学上的空集语义，
     *   hasAllPermission([]) 会返回 true，一旦调用方的权限数组是动态算出来的
     *   且算空了，就等于把入口对所有人打开。
     */

    /**
     * 是否拥有某个角色。角色名是中文，取自 role 表，如 '班主任'、'辅导员'、'学院领导'。
     * 例：v-if="loginStore.hasRole('班主任')"
     */
    hasRole(role: string): boolean {
      const name = trimName(role)
      return name.length > 0 && this.roles.includes(name)
    },

    /**
     * 是否同时拥有传入的全部角色。
     * 例：hasAllRole('班主任', '辅导员')，也可写成 hasAllRole(['班主任', '辅导员'])
     */
    hasAllRole(...roles: Array<string | string[]>): boolean {
      const wanted = toNameList(roles)
      if (wanted.length === 0) return false
      const owned = this.roles
      return wanted.every((name) => owned.includes(name))
    },

    /**
     * 是否拥有传入角色中的任意一个。
     * 对应后端 @RequiresPermissions(value = {...}, logical = Logical.OR) 的语义。
     * 例：hasAnyRole('班主任', '辅导员')
     */
    hasAnyRole(...roles: Array<string | string[]>): boolean {
      const wanted = toNameList(roles)
      if (wanted.length === 0) return false
      const owned = this.roles
      return wanted.some((name) => owned.includes(name))
    },

    /**
     * 是否拥有某个权限。权限串形如 'baseinfo:college:add'，取自 menu 表。
     * 例：v-if="loginStore.hasPermission('baseinfo:college:add')"
     */
    hasPermission(permission: string): boolean {
      const name = trimName(permission)
      return name.length > 0 && this.permissions.includes(name)
    },

    /**
     * 是否同时拥有传入的全部权限（对应 @RequiresPermissions 默认的 Logical.AND）。
     * 例：hasAllPermission('baseinfo:class:add', 'baseinfo:class:update')
     */
    hasAllPermission(...permissions: Array<string | string[]>): boolean {
      const wanted = toNameList(permissions)
      if (wanted.length === 0) return false
      const owned = this.permissions
      return wanted.every((name) => owned.includes(name))
    },

    /**
     * 是否拥有传入权限中的任意一个。
     * 例：hasAnyPermission('leave:banzhuren:access', 'leave:fudaoyuan:access')
     */
    hasAnyPermission(...permissions: Array<string | string[]>): boolean {
      const wanted = toNameList(permissions)
      if (wanted.length === 0) return false
      const owned = this.permissions
      return wanted.some((name) => owned.includes(name))
    },
  },

  getters: {
    // 是否已登录
    isLoggedIn: (state) => !!state.token,

    // 登录用户名
    loginUserName: (state) => {
      if (!state.userInfo) {
        return '用户未登录'
      }

      if (state.userInfo.type === 1 && state.userInfo.student) {
        return state.userInfo.student.name
      }

      if (state.userInfo.type === 2 && state.userInfo.teacher) {
        return state.userInfo.teacher.name
      }

      return '未知用户'
    },

    /**
     * 当前登录用户的角色名数组。后端是 Set<String>，序列化后为数组；
     * 未登录或旧缓存里没有该字段时是空数组，调用方不用判 null。
     */
    roles: (state): string[] => normalizeNames(state.userInfo?.roleNames),

    /**
     * 当前登录用户的权限名数组，形如 'baseinfo:college:add'。
     * 同上，兜底为空数组。
     */
    permissions: (state): string[] => normalizeNames(state.userInfo?.permissionNames),
  },
})
