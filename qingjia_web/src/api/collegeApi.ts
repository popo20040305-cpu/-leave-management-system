import request from '@/utils/request'
import type { College, CollegeForm, CollegeQuery } from '@/type/collegePojo'
import type { PageQuery, PageResult } from '@/type/page'

/**
 * 学院管理接口，对应后端 ApiCollegeController（/api/college/**）。
 *
 * 相对这个文件最初的版本改了两处：
 * 1. 去掉了 IndexResult 包装。原来的 { page, pageUrl } 是对着老 Thymeleaf 页面设计的：
 *    pageUrl 由后端 PageUrlFilter 拦截器拼出来，专门给 templates/common/pageCommon.html
 *    那个分页片段生成 <a href> 用。SPA 里分页条由 Pagination 组件自己算页码，
 *    pageUrl 既拿不到也没处用，所以后端现在直接把 PageResult 放在 data 里返回。
 * 2. 分页参数从 curPage 改成 current + size，与 src/type/page.ts 的 PageQuery 对齐 ——
 *    usePageTable 发出去的就是这两个字段名，curPage 对不上；而且老写法没带 size，
 *    前端选“每页 50 条”后端也不知道。
 *
 * 权限：全部接口都受后端 Shiro 保护，权限串与 menu 表一致
 *   列表 baseinfo:college:access   详情 baseinfo:college:query
 *   新增 baseinfo:college:add      修改 baseinfo:college:update
 *   删除 baseinfo:college:delete
 * /save 由后端按 id 是否为空动态判定要 add 还是 update 权限。
 * 没权限时返回 code=403，request.ts 的拦截器会弹提示；页面上的按钮也要用
 * loginStore.hasPermission 同步隐藏，否则用户点了才看到“没权限”。
 *
 * 返回值已经被 request.ts 的响应拦截器剥过一层：拿到的是 R 里的 data 字段，
 * 不是 { code, msg, data } 整个响应体。
 */

// 分页条件查询（name / num 都是模糊匹配，可省略）
export const getCollegeList = (params: CollegeQuery & PageQuery) => {
  return request.get<PageResult<College>>('/api/college/index', { params })
}

// 新增/修改（id 为 null 走新增，否则走修改）
export const saveCollege = (data: CollegeForm) => {
  return request.post<College>('/api/college/save', data)
}

// 根据ID 获取详情
export const getCollegeInfo = (id: number) => {
  return request.get<College>(`/api/college/info/${id}`)
}

// 删除（注意：必须命名导出）
export const deleteCollege = (id: number) => {
  return request.delete<null>(`/api/college/delete/${id}`)
}
