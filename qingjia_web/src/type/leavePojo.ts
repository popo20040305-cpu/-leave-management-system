/**
 * 请假流程的类型定义，对应后端 vo.LeaveApplicationVo / vo.LeaveApplicationShenPiVo。
 *
 * 状态机见 application_status 表：
 *   1 待提交 -> 2 待班主任审批 -> (3 待辅导员审批) -> (4 待二级学院领导审批)
 *   -> 5 待销假 -> 7 归档；任一环节不同意 -> 6 审批未通过
 *
 * 括号里的两级是条件分支，跟请假类型有关：
 *   班主任同意后：类型 1（一天以内）直接到 5 待销假，其它类型进 3
 *   辅导员同意后：类型 2（一天以外三天以内）直接到 5 待销假，类型 3/4 进 4
 *   院领导同意后：一律到 5 待销假
 * 所以前端不要自己算「下一步状态」，一切以后端返回的 status 为准。
 *
 * 时间字段（begintime / endtime / xiaoJiaTime / createTime）后端统一按
 * 'yyyy-MM-dd HH:mm:ss' 收发（见 application.yml 的 spring.jackson.date-format），
 * 前端直接当字符串用，和 el-date-picker 的 value-format 正好对上。
 */

/** 一条审批记录 */
export interface LeaveShenPi {
  id?: number
  applicationId?: number
  teacherId?: number
  /** 审批教师姓名（后端 join 出来的） */
  teacherName?: string | null
  /** 1 同意 / 2 不同意 */
  result?: number | null
  opinion?: string | null
  createTime?: string | null
  /** 审批人角色：1 班主任 / 2 辅导员 / 3 学院领导 */
  roleType?: number | null
}

/** 请假申请（列表行 / 详情都用它） */
export interface LeaveApplicationVo {
  id?: number
  studentId?: number
  /** 学生姓名；「我的申请」页面不返回（本人不需要），审批页面会返回 */
  studentName?: string | null
  /** 班级编号（class.num） */
  className?: string | null
  /** 请假类型 id，对应 leave_application_type.id */
  type?: number | null
  typeName?: string | null
  /** 请假途径，如「学校到舞阳坝」 */
  path?: string | null
  reason?: string | null
  begintime?: string | null
  endtime?: string | null
  status?: number | null
  statusName?: string | null
  /** 销假照片相对路径，形如 /xiaojiaPhoto/xiaojia2025001_3.jpg */
  xiaoJiaPhotoUrl?: string | null
  xiaoJiaTime?: string | null
  /** 审批记录，按 createTime 升序 */
  shenPiList?: LeaveShenPi[] | null
}

/**
 * 新增 / 修改草稿的提交体。
 *
 * 只带业务字段：studentId 由后端从登录用户取（前端传了也会被覆盖），
 * status 由后端按「存草稿 / 直接提交」自己定，xiaoJia* 只在销假时用。
 */
export interface LeaveForm {
  /** 新增时为 null；修改草稿时为草稿 id */
  id: number | null
  type: number | null
  path: string
  reason: string
  begintime: string
  endtime: string
}

/** 审批页面的查询条件 */
export interface LeaveApproveQuery {
  /** 申请状态，不传看全部 */
  status?: number
  /** 年级（辅导员 / 院领导页面用） */
  grade?: number
  /** 班级 */
  classId?: number
}

/** 审批提交体。teacherId 由后端取登录教师，前端不用也不能传 */
export interface LeaveApproveForm {
  applicationId: number
  /** 1 同意 / 2 不同意 */
  result: number
  opinion: string
}

/** 申请状态常量，与 application_status 表一致 */
export const LEAVE_STATUS = {
  DRAFT: 1,
  BANZHUREN: 2,
  FUDAOYUAN: 3,
  YUANLINGDAO: 4,
  XIAOJIA: 5,
  REJECTED: 6,
  ARCHIVED: 7,
} as const

/** 各角色审批页面的默认筛选状态：进来先看「等我批的」 */
export const LEAVE_PENDING_STATUS = {
  banzhuren: LEAVE_STATUS.BANZHUREN,
  fudaoyuan: LEAVE_STATUS.FUDAOYUAN,
  yuanlingdao: LEAVE_STATUS.YUANLINGDAO,
} as const

/** 状态对应的 el-tag 颜色 */
export function leaveStatusTagType(
  status?: number | null,
): 'info' | 'warning' | 'success' | 'danger' | 'primary' {
  switch (status) {
    case LEAVE_STATUS.DRAFT:
      return 'info'
    case LEAVE_STATUS.BANZHUREN:
    case LEAVE_STATUS.FUDAOYUAN:
    case LEAVE_STATUS.YUANLINGDAO:
      return 'warning'
    case LEAVE_STATUS.XIAOJIA:
      return 'primary'
    case LEAVE_STATUS.REJECTED:
      return 'danger'
    case LEAVE_STATUS.ARCHIVED:
      return 'success'
    default:
      return 'info'
  }
}

/** 审批人角色文案 */
export function leaveRoleText(roleType?: number | null): string {
  switch (roleType) {
    case 1:
      return '班主任'
    case 2:
      return '辅导员'
    case 3:
      return '学院领导'
    default:
      return '审批人'
  }
}

/** 审批结果文案 */
export function leaveResultText(result?: number | null): string {
  if (result === 1) return '同意'
  if (result === 2) return '不同意'
  return '-'
}

/** 空表单 */
export function emptyLeaveForm(): LeaveForm {
  return { id: null, type: null, path: '', reason: '', begintime: '', endtime: '' }
}
