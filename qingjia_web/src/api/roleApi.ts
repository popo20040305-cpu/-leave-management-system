import request from '@/utils/request'
import type { PageQuery, PageResult } from '@/type/page'
import type { PermTreeResult, RolePojo } from '@/type/rolePojo'

/**
 * 角色管理接口，对应后端 ApiRoleController（/api/role/**）。
 *
 * 权限串与 menu 表一致：
 *   查询 system:role:query    添加 system:role:add    修改 system:role:update
 *   删除 system:role:delete   指定角色权限 system:role:zdjsqx
 */

/** 分页查询角色列表。name 模糊匹配角色名 */
export const getRoleList = (params: PageQuery & { name?: string }) => {
  return request.get<PageResult<RolePojo>>('/api/role/index', { params })
}

/** 保存角色。带 id 是修改，不带是新增（后端据此判定 add / update 权限） */
export const saveRole = (data: RolePojo) => {
  return request.post<RolePojo>('/api/role/save', data)
}

/** 角色详情（编辑回显） */
export const getRoleInfo = (id: number) => {
  return request.get<RolePojo>(`/api/role/info/${id}`)
}

/** 删除角色（后端连带清掉 role_menu / user_role 关联） */
export const deleteRole = (id: number) => {
  return request.delete<null>(`/api/role/delete/${id}`)
}

/** 角色权限树：完整菜单树 + 该角色已勾选的叶子权限 id */
export const getPermTree = (roleId: number) => {
  return request.get<PermTreeResult>(`/api/role/permTree/${roleId}`)
}

/**
 * 保存角色权限。
 *
 * menuIds 要以「menuIds=1&menuIds=2」这种重复 key 的形式发，
 * Spring 的 @RequestParam List<Integer> 才认。axios 默认会把数组序列化成
 * menuIds[]=1&menuIds[]=2（多个 [] 的下标风格），后端收到的是 null，
 * 所以这里手动拼 query string。
 */
export const savePerm = (roleId: number, menuIds: number[]) => {
  const searchParams = new URLSearchParams()
  searchParams.append('roleId', String(roleId))
  menuIds.forEach((id) => searchParams.append('menuIds', String(id)))
  return request.post<null>(`/api/role/savePerm?${searchParams.toString()}`)
}

/** 全部角色（不分页），给「指定角色」这类下拉用 */
export const listAllRoles = () => {
  return request.get<RolePojo[]>('/api/role/listAll')
}
