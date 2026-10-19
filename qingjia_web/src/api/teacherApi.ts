import request from '@/utils/request'
import type {
  TeacherForm,
  TeacherQuery,
  TeacherRolesData,
  TeacherVo,
} from '@/type/teacherPojo'
import type { PageQuery, PageResult } from '@/type/page'

/**
 * 教师管理接口，对应后端 ApiTeacherController（/api/teacher/**）。
 *
 * 权限串与 menu 表一致（注意「查询教师」在库里就是拼错的 qurey）：
 *   列表 baseinfo:teacher:access   详情 baseinfo:teacher:qurey
 *   新增 baseinfo:teacher:add      修改 baseinfo:teacher:update
 *   删除 baseinfo:teacher:delete   重置密码 baseinfo:teacher:updatepassword
 *   指定角色 baseinfo:teacher:zdjs
 * /save 由后端按 id 是否为空动态判定要 add 还是 update 权限。
 */

/** 分页条件查询 */
export const getTeacherList = (params: TeacherQuery & PageQuery) => {
  return request.get<PageResult<TeacherVo>>('/api/teacher/index', { params })
}

/** 教师详情（修改弹窗回显，不含 password/salt） */
export const getTeacherInfo = (id: number) => {
  return request.get<TeacherVo>(`/api/teacher/info/${id}`)
}

/** 新增 / 修改（id 为 null 走新增，否则走修改） */
export const saveTeacher = (data: TeacherForm) => {
  return request.post<TeacherVo>('/api/teacher/save', data)
}

/** 删除教师（还是某个班的班主任时后端会拒绝） */
export const deleteTeacher = (id: number) => {
  return request.delete<null>(`/api/teacher/delete/${id}`)
}

/** 重置教师密码 */
export const resetTeacherPassword = (data: { teacherId: number; newPassword: string }) => {
  return request.post<null>('/api/teacher/resetPassword', data)
}

/** 全部角色 + 该教师已勾选的角色 */
export const getTeacherRoles = (id: number) => {
  return request.get<TeacherRolesData>(`/api/teacher/${id}/roles`)
}

/** 保存教师角色（全量覆盖，空数组 = 收回全部角色） */
export const saveTeacherRoles = (id: number, roleIds: number[]) => {
  return request.post<null>(`/api/teacher/${id}/roles`, { roleIds })
}

/**
 * 上传教师头像，返回头像相对路径（/teacherPhoto/xxx.jpg）。
 *
 * 用 FormData 直接发：axios 1.x 在浏览器环境遇到 FormData 会把 Content-Type
 * 让给浏览器自己设置（要带 boundary），所以这里不用、也不能手动写
 * 'multipart/form-data' —— 手写反而会丢掉 boundary，后端解析不出来。
 */
export const uploadTeacherPhoto = (file: File, num?: string) => {
  const formData = new FormData()
  formData.append('photo', file)
  if (num) formData.append('num', num)
  return request.post<string>('/api/teacher/uploadPhoto', formData)
}
