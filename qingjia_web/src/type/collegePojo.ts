/**
 * 学院管理的类型定义。
 *
 * 注意与 '@/type/BaseInfoType' 里的 CollegePojo 区分：
 * - CollegePojo 是「字典」用途（下拉框选项），字段全部可选，因为基础数据接口
 *   返回的对象不保证每个字段都有值；
 * - 这里的 College 是「列表/表单」用途，对应 college 表的一行。
 *
 * 两者对应同一张 college 表，只是使用场景不同。
 */

/** 学院，对应后端 CollegePojo / college 表 */
export interface College {
  /** 新增时前端不传，后端 save 之后回填；列表和详情里一定有值 */
  id?: number
  /** 学院名称，库内唯一 */
  name: string
  /** 学院编号，库内唯一 */
  num: string
  /**
   * 办公电话；没填时后端存 null。
   * 允许留空，否则 3-20 位，可含数字、空格、-、()、+，与后端 CollegePojo.tel 上的
   * @Pattern 保持一致（库里本来就有 '0718 - 8437432' 这类带分隔符的号码）。
   */
  tel?: string
}

/**
 * 新增 / 修改弹窗提交的表单，对应后端 POST /api/college/save 的请求体。
 *
 * 和 College 分开定义是因为两者对「空」的表达方式不一样：
 * 表单里 tel 统一用空串表示「没填」（el-input 的 v-model 天然是空串，
 * 让 null 进来反而要在提交前多转一次），后端 normalize 时会转成 null 入库；
 * id 用 null 表示新增，后端正是靠 id 是否为空来区分新增还是修改，
 * 并据此校验 baseinfo:college:add 或 baseinfo:college:update 权限。
 */
export interface CollegeForm {
  /** 新增时为 null；修改时为要改的那条记录的 id */
  id: number | null
  name: string
  num: string
  /** 允许为空串，提交时后端会转成 null */
  tel: string
}

/**
 * 列表查询条件，对应后端 GET /api/college/index 的 name / num（都是模糊匹配）。
 * 两个字段都可以省略，省略即不参与过滤。
 * 实际发请求时与 '@/type/page' 的 PageQuery（current + size）做交叉类型。
 */
export interface CollegeQuery {
  name?: string
  num?: string
}

/** 空表单，用于打开「新增」弹窗和提交成功后重置 */
export function emptyCollegeForm(): CollegeForm {
  return { id: null, name: '', num: '', tel: '' }
}
