/**
 * 学生管理的类型定义，对应后端 pojo.StudentPojo / vo.StudentVo（student 表）。
 *
 * StudentVo 比 StudentPojo 多出 join 出来的 className（班级编号）、grade（年级）、
 * collegeId / collegeName（学院），前端表格和详情直接用这些显示名。
 *
 * 注意：后端返回的对象里**没有** password / salt —— studentMapper.xml 的
 * student_query_field 不查这两列。
 */

/** 学生列表行，对应后端 StudentVo */
export interface StudentVo {
  id?: number
  name: string
  num: string
  classId?: number | null
  /** 班级编号（class.num），不是班级名 */
  className?: string | null
  /** 性别字典 id，对应 sex_type.id */
  sex?: number | null
  mobile?: string | null
  qq?: string | null
  /** 头像相对路径，形如 /studentPhoto/2025001_1700000000000.jpg */
  avatarUrl?: string | null
  /** 年级（class.grade），只读，由班级决定 */
  grade?: number | null
  /** 学院（class.belongToCollegeId / college.id），只读，由班级决定 */
  collegeId?: number | null
  collegeName?: string | null
}

/**
 * 新增 / 修改弹窗的表单。
 *
 * 年级和学院不是独立字段：库表里只有 student.classId，年级/学院是顺着
 * class 表 join 出来的。所以表单里选它们只是为了联动筛出班级，
 * 提交时只带 classId（学院、年级由后端 join 得到，不能写）。
 *
 * password 只在新增时用，理由同 teacherPojo.TeacherForm。
 */
export interface StudentForm {
  /** 新增时为 null；修改时为要改的那条记录的 id */
  id: number | null
  name: string
  num: string
  classId: number | null
  sex: number | null
  /** 空串表示没填，后端会归一成 null */
  mobile: string
  qq: string
  avatarUrl: string | null
  password: string
}

/**
 * 列表查询条件，对应后端 GET /api/student/index。
 * num / name 模糊匹配；collegeId / grade / classId 精确匹配
 * （前两个实际过滤的是 join 出来的 class.grade 和 college.id）。
 */
export interface StudentQuery {
  num?: string
  name?: string
  collegeId?: number
  grade?: number
  classId?: number
}

/** 空表单，用于打开「新增」弹窗和提交成功后重置 */
export function emptyStudentForm(): StudentForm {
  return {
    id: null,
    name: '',
    num: '',
    classId: null,
    sex: null,
    mobile: '',
    qq: '',
    avatarUrl: null,
    password: '',
  }
}
