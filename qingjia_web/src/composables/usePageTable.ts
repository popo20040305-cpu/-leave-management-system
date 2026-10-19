import { reactive, ref, type Ref } from 'vue'
import { type PageQuery, type PageResult } from '@/type/page'
import { getStoredPageSize } from '@/api/pageSizeApi'

export interface UsePageTableOptions<Q> {
  /** 每页条数，默认取本地上次记住的值（没有则 10） */
  size?: number
  /** 创建时是否立刻查一次，默认 true */
  immediate?: boolean
  /** 查询条件初始值，同时也是 reset() 回退到的值 */
  query?: Q
}

/**
 * 列表页通用逻辑：查询条件 + 分页 + loading + 拉数据，一次配好。
 *
 * 存在的意义是别让每个列表页都重抄一遍这二十来行 —— 抄的时候最容易漏的就是
 * “改了每页条数要回到第一页”和“删掉当前页最后一条要往前退一页”这两个边界。
 *
 * 用法：
 *   const page = usePageTable<TeacherVo, { name?: string }>(
 *     (params) => pageTeachersApi(params),
 *     { query: { name: '' } },
 *   )
 *
 *   <el-table :data="page.records" v-loading="page.loading"> ... </el-table>
 *   <Pagination v-model:current="page.current" v-model:size="page.size"
 *               :total="page.total" @change="page.onPageChange" />
 *   <el-button @click="page.search">查询</el-button>
 *   <el-button @click="page.reset">重置</el-button>
 *
 * fetcher 需要接收「查询条件 + current + size」，返回后端统一的 PageResult。
 *
 * 注意：返回值是 reactive 对象，**不要解构**（`const { records } = usePageTable(...)`
 * 拿到的是一份失去响应式的快照），一律整体用 `page.xxx`。详见 return 处的注释。
 */
export function usePageTable<T, Q extends object = Record<string, unknown>>(
  fetcher: (params: Q & PageQuery) => Promise<PageResult<T>>,
  options: UsePageTableOptions<Q> = {},
) {
  // 没显式传 size 时用本地记住的每页条数，让用户上次的偏好跨页面、跨会话生效
  const { size = getStoredPageSize(), immediate = true, query } = options

  const initialQuery = { ...(query ?? {}) } as Record<string, unknown>
  const form = reactive({ ...initialQuery }) as Q

  const records = ref<T[]>([]) as Ref<T[]>
  const total = ref(0)
  const current = ref(1)
  const pageSize = ref(size)
  const loading = ref(false)

  const load = async (): Promise<void> => {
    loading.value = true
    try {
      const params = { ...form, current: current.value, size: pageSize.value } as Q & PageQuery
      const res = await fetcher(params)
      records.value = res?.records ?? []
      total.value = res?.total ?? 0
      // 页码/每页条数以服务端返回的为准：删掉最后一页的数据后，
      // 后端可能把 current 纠正回去了，本地跟着改才不会停在空页上
      if (res?.current) current.value = res.current
      if (res?.size) pageSize.value = res.size
    } catch (error) {
      // request.ts 的响应拦截器已经弹过错误提示了，这里只把表格恢复成空态。
      // 故意不往上抛：列表加载失败不该让整个页面崩掉。
      console.error('列表加载失败:', error)
      records.value = []
      total.value = 0
    } finally {
      loading.value = false
    }
  }

  /** 直接绑到 Pagination 的 @change 上 */
  const onPageChange = (payload: { current: number; size: number }): Promise<void> => {
    current.value = payload.current
    pageSize.value = payload.size
    return load()
  }

  /** 点“查询”：带上当前条件，回到第一页 */
  const search = (): Promise<void> => {
    current.value = 1
    return load()
  }

  /** 点“重置”：条件回到初始值并回到第一页 */
  const reset = (): Promise<void> => {
    const target = form as Record<string, unknown>
    Object.keys(target).forEach((key) => {
      target[key] = initialQuery[key]
    })
    current.value = 1
    return load()
  }

  /**
   * 删除一条记录之后调用。如果删的是当前页最后一条且不是第一页，
   * 就往前退一页再查，否则会停在一个空页上。
   */
  const reloadAfterDelete = (): Promise<void> => {
    if (records.value.length <= 1 && current.value > 1) {
      current.value -= 1
    }
    return load()
  }

  if (immediate) {
    void load()
  }

  // 必须用 reactive 包一层再返回，不能直接返回普通对象。
  //
  // 原因：<script setup> 只自动解包「顶层」ref。上面这些 ref 装进普通对象后，
  // 模板里的 page.records 拿到的是 Ref 对象本身而不是数组：
  //   - el-table 立刻报 “Invalid prop: type check failed for prop "data".
  //     Expected Array, got Object”，紧接着 rows is not iterable，整页渲染失败；
  //   - v-model:current="page.current" 会把 ref 对象整个覆盖成一个数字，
  //     响应式链路直接断掉，翻页从此失效。
  // reactive 会解包嵌套 ref，于是模板里和 script 里 page.records / page.current
  // 拿到的都是值本身，两边行为一致。
  return reactive({
    /** 查询条件表单，直接 v-model 绑到搜索栏 */
    form,
    records,
    total,
    current,
    /** 每页条数，绑 Pagination 的 v-model:size */
    size: pageSize,
    loading,
    load,
    search,
    reset,
    onPageChange,
    reloadAfterDelete,
  })
}
