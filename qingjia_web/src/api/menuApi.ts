import request from '@/utils/request'
import type { MenuPojo, MenuVo } from '@/type/menu'

/**
 * 菜单相关接口，对应后端 ApiMenuController（/api/menu/**）。
 *
 * getMenuList 是主框架左侧菜单用的（按登录用户的角色过滤，只有目录和菜单两层）；
 * 其余五个是「系统管理 -> 权限管理」页面用的完整菜单树增删改查 + 同级移动。
 */

/** 主框架左侧菜单（目录 + 菜单，已按当前用户角色过滤） */
export const getMenuList = () => {
  return request.get<{ menuVos: MenuVo[] }>('/api/menu/left.html')
}

/** 完整菜单树（目录 -> 菜单 -> 按钮，三层，不做角色过滤，给管理页用） */
export const getMenuTree = () => {
  return request.get<MenuVo[]>('/api/menu/list')
}

/**
 * 保存菜单的请求体。
 * id 允许缺省或 null（新增），其余字段来自 MenuPojo。
 * 单独定义这个类型是为了不把 MenuPojo.id 从 number 放宽成 number | null ——
 * 树节点、路由生成那几处都按「一定有 id」在用。
 */
export type MenuSaveForm = Omit<MenuPojo, 'id'> & { id?: number | null }

/** 保存菜单。带 id 是修改，不带是新增（后端据此判定 add / update 权限） */
export const saveMenu = (data: MenuSaveForm) => {
  return request.post<MenuPojo>('/api/menu/save', data)
}

/** 单个菜单详情（编辑回显） */
export const getMenuInfo = (id: number) => {
  return request.get<MenuPojo>(`/api/menu/info/${id}`)
}

/** 删除菜单（后端会级联删除所有子孙） */
export const deleteMenu = (id: number) => {
  return request.delete<null>(`/api/menu/delete/${id}`)
}

/** 同级上移 / 下移，靠交换相邻节点的 order_num 实现 */
export const moveMenu = (id: number, direction: 'up' | 'down') => {
  return request.post<null>('/api/menu/move', null, { params: { id, direction } })
}
