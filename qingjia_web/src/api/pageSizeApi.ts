import request from '@/utils/request'
import { DEFAULT_PAGE_SIZE, DEFAULT_PAGE_SIZES } from '@/type/page'
import { storage } from '@/utils/storage'

/**
 * 每页条数的「用户偏好」（文档 10.2）。
 *
 * 存两份，各管一段：
 * - localStorage（本文件）：管前端首屏。下次打开页面 usePageTable 直接用它当默认 size，
 *   不用等后端返回才知道每页显示几条。
 * - 后端 session（/api/setPageSize）：管后端兜底。后端部分接口在 size 参数缺失时
 *   会回落到 BaseController.getPageSize()，那里读的就是 session。
 *
 * 两份不一致时以 localStorage 为准 —— 它是用户最近一次操作留下的，更新鲜。
 */

const SIZE_STORAGE_KEY = 'pageSize'

/** 读取本地上次记住的每页条数，没有或非法则回默认 10 */
export const getStoredPageSize = (): number => {
  const saved = storage.get<number>(SIZE_STORAGE_KEY)
  if (typeof saved !== 'number' || !Number.isFinite(saved)) return DEFAULT_PAGE_SIZE
  if (!DEFAULT_PAGE_SIZES.includes(saved)) return DEFAULT_PAGE_SIZE
  return saved
}

/** 记住每页条数（本地 + 服务端 session）。失败不抛错，不影响列表刷新 */
export const setPageSize = async (pageSize: number): Promise<void> => {
  storage.set(SIZE_STORAGE_KEY, pageSize)
  try {
    await request.get('/api/setPageSize', { params: { pageSize } })
  } catch (error) {
    // 服务端没记住不影响使用：下拉框的选择已经在本地生效了。
    // 这里不往上抛，免得用户切每页条数时弹一个跟列表无关的错误。
    console.warn('服务端保存每页条数失败:', error)
  }
}
