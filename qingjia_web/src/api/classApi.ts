import request from '@/utils/request'
import type { ClassForm, ClassPojo, ClassQuery, ClassVo } from '@/type/classPojo'
import type { PageQuery, PageResult } from '@/type/page'
import type { TeacherOption } from '@/type/Teacher'

/**
 * 班级管理接口，对应后端 ApiClassController（/api/class/**）。
 *
 * 契约和 collegeApi.ts 一致：分页参数是 current + size（不是老页面的 curPage），
 * 列表返回 PageResult 而不是 MyBatis-Plus 的 Page，返回值已被 request.ts 的
 * 响应拦截器剥掉一层 R 壳（拿到的是 R.data）。
 *
 * 权限串与 menu 表一致：
 *   列表 baseinfo:class:access   详情 baseinfo:class:query
 *   新增 baseinfo:class:add      修改 baseinfo:class:update
 *   删除 baseinfo:class:delete   指定班主任 baseinfo:class:banzhuren
 * /save 由后端按 id 是否为空动态判定要 add 还是 update 权限。
 */

/** 分页条件查询 */
export const getClassList = (params: ClassQuery & PageQuery) => {
  return request.get<PageResult<ClassVo>>('/api/class/index', { params })
}

/** 根据 id 获取班级详情（修改弹窗回显） */
export const getClassInfo = (id: number) => {
  return request.get<ClassPojo>(`/api/class/info/${id}`)
}

/** 新增 / 修改（id 为 null 走新增，否则走修改） */
export const saveClass = (data: ClassForm) => {
  return request.post<ClassPojo>('/api/class/save', data)
}

/** 删除班级（班上有学生时后端会拒绝） */
export const deleteClass = (id: number) => {
  return request.delete<null>(`/api/class/delete/${id}`)
}

/**
 * 不分页的班级列表，给学生管理页的「学院 -> 年级 -> 班级」联动下拉用。
 * 两个条件都可省略，省略即全量。权限是 class:access（学生角色也有，用于按班筛同学）。
 */
export const listClassesApi = (params: { collegeId?: number; grade?: number } = {}) => {
  return request.get<ClassPojo[]>('/api/class/list', { params })
}

/**
 * 按姓名模糊搜教师，给「指定班主任」下拉框用。
 * 只返回 id/name/collegeName，不含密码盐值。
 *
 * 注意：后端 name 是 required=false，不传就把全校教师都拉回来，所以界面上
 * 一定要先输入关键字再查。
 */
export const searchTeachersApi = (name: string) => {
  return request.get<TeacherOption[]>('/api/class/teachers', { params: { name } })
}

/**
 * 指定班主任。后端除改 class.banZhuRenId 外，还会同步 user_role 表里的
 * 「班主任」角色（新班主任挂上、旧班主任在其他班也带班时不摘），
 * 所以换班主任必须走这个接口，不能只改字段。
 */
export const setHeadTeacherApi = (data: { classId: number; teacherId: number }) => {
  return request.post<null>('/api/class/setHeadTeacher', data)
}
