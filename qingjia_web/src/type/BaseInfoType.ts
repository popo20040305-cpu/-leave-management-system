export interface GradePojo {
  id?: number // 对应 Integer，自增主键
  grade?: number // 对应 Integer
}

export interface AproveTypePojo {
  id?: number // 对应 Integer，自增主键
  name?: string // 对应 String
}

export interface LeaveApplicationTypePojo {
  id?: number // 对应 Integer，自增主键
  name?: string // 对应 String
}

export interface MenuTypePojo {
  id?: number // 对应 Integer
  name?: string // 对应 String
}

export interface RoleTypePojo {
  id?: number // 对应 Integer
  name?: string // 对应 String
}

export interface SexTypePojo {
  id?: number // 对应 Integer，自增主键
  name?: string // 对应 String
}

export interface ApplicationStatusPojo {
  id?: number // 对应 Integer
  applicationStutas?: string // 对应 String（保留原字段名拼写）
}

/**
 * 学院 —— 后端 CollegePojo。
 * BaseInfoApi.ts 原来是从 '@/type/college' 导入 College 的，但那个文件不存在，
 * type-check 直接报 TS2307，所以补到这里，和其他字典类型放一起。
 */
export interface CollegePojo {
  id?: number // 对应 Integer，自增主键
  name?: string // 学院名
  num?: string // 学院编号
  tel?: string | null // 办公电话
}

/**
 * GET /api/base/all 的返回结构。
 * 字段顺序与后端 ApiBaseDataController.ALL_KEYS 一致。
 */
export interface BaseInfoData {
  colleges: CollegePojo[]
  grades: GradePojo[]
  aproveTypes: AproveTypePojo[]
  leaveApplicationTypes: LeaveApplicationTypePojo[]
  menuTypes: MenuTypePojo[]
  roleTypes: RoleTypePojo[]
  sexTypes: SexTypePojo[]
  applicationStatuses: ApplicationStatusPojo[]
}

export type BaseInfoKey = keyof BaseInfoData

/** 全部字典 key，需要和后端 ALL_KEYS 保持同步 */
export const BASE_INFO_KEYS: BaseInfoKey[] = [
  'colleges',
  'grades',
  'aproveTypes',
  'leaveApplicationTypes',
  'menuTypes',
  'roleTypes',
  'sexTypes',
  'applicationStatuses',
]

/**
 * 空的基础数据。加载前 / 加载失败 / 退出登录后都用它兜底，
 * 保证组件遍历下拉选项时不会遇到 undefined。
 */
export function emptyBaseInfo(): BaseInfoData {
  return {
    colleges: [],
    grades: [],
    aproveTypes: [],
    leaveApplicationTypes: [],
    menuTypes: [],
    roleTypes: [],
    sexTypes: [],
    applicationStatuses: [],
  }
}
