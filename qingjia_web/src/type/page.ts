/**
 * 分页公共类型。对应后端 edu.hbmd.taotao.qingjia.vo.PageResult。
 *
 * 所有分页列表接口统一返回这个结构，后端不要把 MyBatis-Plus 的 Page 直接透出来
 * （Page 会带 orders / optimizeCountSql / searchCount 等一堆前端用不到的内部字段，
 * 而且字段会随 MyBatis-Plus 版本变化）。
 */
export interface PageResult<T> {
  /** 当前页数据 */
  records: T[]
  /** 总记录数 */
  total: number
  /** 当前页码，从 1 开始 */
  current: number
  /** 每页条数 */
  size: number
  /** 总页数 */
  pages: number
}

/** 分页请求参数，业务查询条件与它做交叉类型后一起发给后端 */
export interface PageQuery {
  current: number
  size: number
}

/** 默认每页条数，和老项目 PageConstant.pageSize 保持一致 */
export const DEFAULT_PAGE_SIZE = 10

/** 每页条数可选项 */
export const DEFAULT_PAGE_SIZES = [10, 20, 50, 100]

/**
 * 空分页结果。查询失败或还没开始查时用，
 * 保证组件拿到的对象结构永远完整，不用写 records 可能为 undefined 的防御代码。
 */
export function emptyPage<T>(current = 1, size = DEFAULT_PAGE_SIZE): PageResult<T> {
  return { records: [], total: 0, current, size, pages: 0 }
}
