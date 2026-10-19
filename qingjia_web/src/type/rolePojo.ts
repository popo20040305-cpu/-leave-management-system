import type { MenuVo } from './menu'

/**
 * 角色，对应后端 pojo.RolePojo（role 表）。
 * roleType 是数据访问范围：1 个人 / 2 班级 / 3 学院 / 4 全部（见 RoleTypeConstant）。
 */
export interface RolePojo {
  id?: number
  name: string
  remark?: string
  /** 角色类型 id，取自字典 roleTypes（baseInfoStore） */
  roleType: number
}

/** 空表单，新增时用。roleType 留空逼用户选，避免误建一个「范围不明」的角色 */
export function emptyRoleForm(): RolePojo {
  return { name: '', remark: '', roleType: undefined as unknown as number }
}

/**
 * 权限树接口返回，对应后端 GET /api/role/permTree/{roleId}。
 *
 * checkedIds 只含叶子节点（按钮 / 菜单）：后端把父节点剔掉了，
 * 否则 el-tree 回显时会因为父节点选中而把整棵树勾满。
 */
export interface PermTreeResult {
  tree: MenuVo[]
  checkedIds: number[]
}
