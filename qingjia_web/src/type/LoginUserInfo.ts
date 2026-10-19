import type { Teacher } from './Teacher'
import type { Student } from './Student'

/**
 * 对应后端 edu.hbmd.taotao.qingjia.shiro.LoginUser。
 * 学生登录时 teacher 为 null，教师登录时 student 为 null，所以两个都可空。
 */
export interface LoginUserInfo {
  type: number // 1：学生  2：教师
  student: Student | null
  teacher: Teacher | null
  roleType: number
  roleNames?: string[] | null
  permissionNames?: string[] | null
}
