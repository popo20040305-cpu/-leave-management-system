import request from '@/utils/request'
import type { StudentForm, StudentQuery, StudentVo } from '@/type/studentPojo'
import type { PageQuery, PageResult } from '@/type/page'

/**
 * 学生管理接口，对应后端 ApiStudentController（/api/student/**）。
 *
 * 权限串与 menu 表一致：
 *   列表 baseinfo:student:access   详情 baseinfo:student:query
 *   新增 baseinfo:student:add      修改 baseinfo:student:update
 *   修改密码 baseinfo:student:updatepassword   详情页 baseinfo:student:detail
 *
 * 没有删除接口：menu 表里「学生管理」就没有删除权限，后端也没写。
 *
 * 另外注意数据范围：roleType 为「个人」的学生调这些接口只会拿到自己那一条，
 * 后端在两个入口（列表、详情）都做了限制，前端不用另外处理。
 */

/** 分页条件查询 */
export const getStudentList = (params: StudentQuery & PageQuery) => {
  return request.get<PageResult<StudentVo>>('/api/student/index', { params })
}

/** 学生详情（含班级编号、年级、学院名，不含 password/salt） */
export const getStudentInfo = (id: number) => {
  return request.get<StudentVo>(`/api/student/info/${id}`)
}

/** 新增 / 修改（id 为 null 走新增，否则走修改） */
export const saveStudent = (data: StudentForm) => {
  return request.post<StudentVo>('/api/student/save', data)
}

/** 重置学生密码 */
export const resetStudentPassword = (data: { studentId: number; newPassword: string }) => {
  return request.post<null>('/api/student/resetPassword', data)
}

/**
 * 上传学生头像，返回头像相对路径（/studentPhoto/xxx.jpg）。
 * FormData 的 Content-Type 由浏览器（axios）自己带 boundary，不要手动指定。
 */
export const uploadStudentPhoto = (file: File, num?: string) => {
  const formData = new FormData()
  formData.append('photo', file)
  if (num) formData.append('num', num)
  return request.post<string>('/api/student/uploadPhoto', formData)
}
