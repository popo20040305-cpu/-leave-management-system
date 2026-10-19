import request from '@/utils/request'
import type { ClassPojo } from '@/type/classPojo'
import type { PageQuery, PageResult } from '@/type/page'
import type {
  LeaveApplicationVo,
  LeaveApproveForm,
  LeaveApproveQuery,
  LeaveForm,
} from '@/type/leavePojo'

/**
 * 请假流程接口，对应后端 ApiLeaveController（/api/leave/**）。
 * 三条支线：student（我的申请）/ banzhuren / fudaoyuan / yuanlingdao（三级审批）。
 *
 * 权限串与 menu 表一致：
 *   我的申请 leave:student:access   新增/修改 leave:student:add
 *   详情     leave:student:detail   删除草稿   leave:student:delete
 *   销假     leave:student:xiaojia
 *   班主任   leave:banzhuren:access  辅导员 leave:fudaoyuan:access
 *   院领导   leave:yuanlingdao:access
 */

/** 审批端角色 key，和 URL 段一一对应 */
export type LeaveApprovalRole = 'banzhuren' | 'fudaoyuan' | 'yuanlingdao'

/* ---------------- 我的申请（学生） ---------------- */

/** 分页查询自己的请假申请；status 不传看全部 */
export const getMyLeaveList = (params: PageQuery & { status?: number }) => {
  return request.get<PageResult<LeaveApplicationVo>>('/api/leave/student/index', { params })
}

/** 申请详情（含审批记录），只能查自己的 */
export const getMyLeaveDetail = (id: number) => {
  return request.get<LeaveApplicationVo>(`/api/leave/student/detail/${id}`)
}

/** 存草稿（状态 1 待提交）。带 id 表示改已有草稿 */
export const saveLeaveDraft = (data: LeaveForm) => {
  return request.post<LeaveApplicationVo>('/api/leave/student/saveDraft', data)
}

/** 直接提交 / 改完提交（状态 2 待班主任审批） */
export const submitLeave = (data: LeaveForm) => {
  return request.post<LeaveApplicationVo>('/api/leave/student/submit', data)
}

/** 提交已有草稿 */
export const submitLeaveDraft = (id: number) => {
  return request.post<null>(`/api/leave/student/submitDraft/${id}`)
}

/** 删除草稿（只有待提交状态可删） */
export const deleteLeaveDraft = (id: number) => {
  return request.delete<null>(`/api/leave/student/delete/${id}`)
}

/**
 * 销假：上传销假照片，状态由 5 待销假变为 7 归档。
 * FormData 的 Content-Type 交给浏览器（axios）自己带 boundary。
 */
export const xiaoJiaLeave = (applicationId: number, file: File) => {
  const formData = new FormData()
  formData.append('applicationId', String(applicationId))
  formData.append('photo', file)
  return request.post<null>('/api/leave/student/xiaojia', formData)
}

/* ---------------- 审批端（班主任 / 辅导员 / 院领导） ---------------- */

/** 分页查询待审批 / 已审批的申请 */
export const getLeaveApprovalList = (
  role: LeaveApprovalRole,
  params: PageQuery & LeaveApproveQuery,
) => {
  return request.get<PageResult<LeaveApplicationVo>>(`/api/leave/${role}/index`, { params })
}

/** 申请详情（含审批记录） */
export const getLeaveApprovalDetail = (role: LeaveApprovalRole, id: number) => {
  return request.get<LeaveApplicationVo>(`/api/leave/${role}/detail/${id}`)
}

/**
 * 审批页面的班级下拉。
 * 班主任返回他带的班；辅导员 / 院领导返回本学院（可按年级筛）的班。
 */
export const getLeaveApprovalClasses = (role: LeaveApprovalRole, grade?: number) => {
  return request.get<ClassPojo[]>(`/api/leave/${role}/classes`, { params: { grade } })
}

/** 审批：result 1 同意 / 2 不同意，opinion 审批意见 */
export const approveLeave = (role: LeaveApprovalRole, data: LeaveApproveForm) => {
  return request.post<null>(`/api/leave/${role}/approve`, data)
}
