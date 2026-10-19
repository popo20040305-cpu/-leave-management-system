/**
 * 班级管理的类型定义，对应后端 pojo.ClassPojo / vo.ClassVo（class 表）。
 *
 * 和学院那边一样分三层：
 * - ClassPojo：后端表实体，/api/class/info/{id} 和 /save 的返回体；
 * - ClassVo：列表行，比 ClassPojo 多出 join 出来的 collegeName（学院名）和
 *   bzrName（班主任姓名），前端表格直接显示，不用再拿 id 去字典里翻；
 * - ClassForm：新增/修改弹窗提交的表单。
 */

/** 班级，对应后端 ClassPojo / class 表 */
export interface ClassPojo {
  /** 新增时不传，后端 save 之后回填；列表和详情里一定有值 */
  id?: number
  /**
   * 班级编号。库里是 varchar(10)，且 class 表没做校验 —— 所以前端 maxlength=10，
   * 否则超长会在 DB 层报错（或被静默截断），用户看到的是一个语焉不详的 500。
   */
  num: string
  /** 年级，如 2020。后端是 Integer，没填时库里是 null */
  grade?: number | null
  /** 所属学院 id，对应 college.id */
  belongToCollegeId?: number | null
  /** 班主任（教师）id，对应 teacher.id；未指定时是 null */
  banZhuRenId?: number | null
}

/** 列表行：ClassPojo + 两个 join 出来的显示名 */
export interface ClassVo extends ClassPojo {
  /** 班主任姓名，未指定班主任时为 null */
  bzrName?: string | null
  /** 所属学院名称 */
  collegeName?: string | null
}

/**
 * 新增 / 修改弹窗提交的表单。
 *
 * banZhuRenId 必须一起带上，哪怕弹窗里根本没有这个字段：后端 /save 的修改分支
 * 拿 banZhuRenId 是否为 null 来判断「要不要清空班主任」（见 ApiClassController.save）。
 * 如果这里漏传，每次点「修改班级」都会把班主任悄悄清掉。
 * 正常路径是提交前用详情接口返回的 banZhuRenId 回填，等于原样带回去。
 * 想主动换班主任走列表里的「指定班主任」按钮（/api/class/setHeadTeacher）。
 */
export interface ClassForm {
  /** 新增时为 null；修改时为要改的那条记录的 id */
  id: number | null
  num: string
  /** 必填（库表 NOT NULL），所以表单里用 null 表示「还没选」 */
  belongToCollegeId: number | null
  grade: number | null
  banZhuRenId: number | null
}

/**
 * 列表查询条件，对应后端 GET /api/class/index。
 * 四个条件都可省略，省略即不参与过滤。
 * 实际发请求时与 '@/type/page' 的 PageQuery（current + size）做交叉类型。
 */
export interface ClassQuery {
  /** 班级编号，模糊匹配 */
  num?: string
  /** 年级，精确匹配 */
  grade?: number
  /** 所属学院 id，精确匹配 */
  belongToCollegeId?: number
  /** 班主任 id，精确匹配（暂未在界面上暴露成筛选项） */
  banZhuRenId?: number
}

/** 空表单，用于打开「新增」弹窗和提交成功后重置 */
export function emptyClassForm(): ClassForm {
  return { id: null, num: '', belongToCollegeId: null, grade: null, banZhuRenId: null }
}
