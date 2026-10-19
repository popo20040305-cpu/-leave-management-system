export interface Teacher {
    id: number;
    name: string;
    num: string;
    collegeId: number;
    collegeName: string;
    mobile: string;
    avatarUrl: string;
}

/**
 * 「指定班主任」下拉框用的精简教师对象。
 *
 * 对应后端 ApiClassController./api/class/teachers，走的是 teacherMapper.xml 里的
 * listBaseInfoTeachers —— 只 select id/name/collegeId/collegeName，不含工号、手机号、
 * 头像，更不含 password/salt。所以不要用上面的 Teacher（它要求 num/mobile/avatarUrl
 * 都是必填，拿这个接口的数据去填会直接类型不通过）。
 */
export interface TeacherOption {
  id: number
  name: string
  collegeId?: number | null
  collegeName?: string | null
}
