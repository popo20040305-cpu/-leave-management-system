/**
 * 教师管理的类型定义，对应后端 pojo.TeacherPojo / vo.TeacherVo（teacher 表）。
 *
 * 注意：后端返回的对象里**没有** password / salt —— teacherMapper.xml 的
 * teacher_query_field 不查这两列，/api/teacher/info 也会在返回前把它们清空。
 */

/** 教师列表行，对应后端 TeacherVo（比 TeacherPojo 多一个 join 出来的学院名） */
export interface TeacherVo {
  id?: number
  name: string
  num: string
  collegeId?: number | null
  collegeName?: string | null
  mobile?: string | null
  /** 头像相对路径，形如 /teacherPhoto/1001.jpg，展示时要拼上服务端前缀 */
  avatarUrl?: string | null
}

/**
 * 新增 / 修改弹窗的表单。
 *
 * password 只在新增时用：后端新增分支会生成私盐并加盐哈希，缺密码直接 400；
 * 修改分支走 updateById + MyBatis-Plus 的 NOT_NULL 字段策略，
 * 传 null 的字段会被跳过，所以改资料不会把密码或头像清空。
 */
export interface TeacherForm {
  /** 新增时为 null；修改时为要改的那条记录的 id */
  id: number | null
  name: string
  num: string
  collegeId: number | null
  /** 空串表示没填，提交时后端会归一成 null */
  mobile: string
  avatarUrl: string | null
  password: string
}

/**
 * 列表查询条件，对应后端 GET /api/teacher/index。
 * num / name 模糊匹配，collegeId 精确匹配，都可省略。
 */
export interface TeacherQuery {
  num?: string
  name?: string
  collegeId?: number
}

/** 角色（后端 RolePojo），「指定教师角色」弹窗的勾选项 */
export interface RoleOption {
  id?: number
  name: string
  remark?: string | null
  /** 数据访问范围：1 个人 / 2 班级 / 3 学院 / 4 全部 */
  roleType?: number
}

/** GET /api/teacher/{id}/roles 的返回体（已被拦截器剥掉 R 壳） */
export interface TeacherRolesData {
  allRoles: RoleOption[]
  /** 该教师当前已勾选的角色 id */
  checkedIds: number[]
}

/** 空表单，用于打开「新增」弹窗和提交成功后重置 */
export function emptyTeacherForm(): TeacherForm {
  return {
    id: null,
    name: '',
    num: '',
    collegeId: null,
    mobile: '',
    avatarUrl: null,
    password: '',
  }
}
