/**
 * 学院管理的 Pinia store。
 *
 * 说明：当前「学院管理」页面（views/college/CollegeIndex.vue）直接把逻辑写在组件里、
 * 用的是公共的 usePageTable，并没有接这个 store —— 它现在是「备用/给别的页面复用」的状态。
 * 保留它是因为「教师管理」「班级管理」这类页面迟早要读写学院数据，抽成 store 比各页面
 * 各拉一遍更省请求。
 *
 * 上一版是个过期残留，引用了几处早就不存在的东西，导致 vue-tsc 直接报错：
 * 1. `import { Page } from '@/type/page'` —— 那个模块只导出 PageResult / PageQuery，
 *    从来没有 Page 类，报 TS2305；配套的 `Page.fromRaw(res.page)` 自然也不成立。
 * 2. 分页参数用 curPage，而 collegeApi / 后端 ApiCollegeController 收的是 current + size，
 *    报 TS2353。
 * 3. changePageSize 调用了 `setPageSize`，但那个 api 的 import 被注释掉了（后端
 *    /setPageSize 是给老 Thymeleaf 页面改全局 PageConstant 用的，SPA 不需要），
 *    报 TS2304。
 * 4. `College.id` 是可选的，直接 Object.assign 进 CollegeForm 的 id（number | null）
 *    会报 TS2345。
 *
 * 这一版按 PageResult + current/size 重写，和 collegeApi.ts 的签名对齐；
 * 表单统一用 CollegeForm（id 为 null 表示新增），不再混用 College。
 */
import { computed, reactive, ref } from 'vue'
import { defineStore } from 'pinia'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteCollege, getCollegeInfo, getCollegeList, saveCollege } from '@/api/collegeApi'
import { emptyCollegeForm, type College, type CollegeForm } from '@/type/collegePojo'
import { DEFAULT_PAGE_SIZE, emptyPage, type PageResult } from '@/type/page'

export const useCollegeStore = defineStore('college', () => {
  // -------- 状态 --------
  const page = ref<PageResult<College>>(emptyPage<College>())
  const loading = ref(false)

  // 查询条件
  const queryParams = reactive({
    name: '',
    num: '',
  })

  // 表单数据。id 为 null 表示「新增」，后端就是靠这个区分 add / update 权限的
  const formData = reactive<CollegeForm>(emptyCollegeForm())

  const formVisible = ref(false)
  const isEdit = ref(false)

  // -------- 辅助计算属性 --------
  const total = computed(() => page.value?.total ?? 0)
  const currentPage = computed(() => page.value?.current ?? 1)
  const pageSize = computed(() => page.value?.size ?? DEFAULT_PAGE_SIZE)
  const list = computed(() => page.value?.records ?? [])
  /** 还有上一页 / 下一页。PageResult 里没有这两个字段，按页码和总页数自己算 */
  const hasPrevious = computed(() => page.value.current > 1)
  const hasNext = computed(() => page.value.current < page.value.pages)

  // -------- 操作 --------
  // 获取列表
  const fetchList = async (params: { current?: number; size?: number; name?: string; num?: string } = {}) => {
    loading.value = true
    try {
      const res = await getCollegeList({
        current: params?.current ?? currentPage.value,
        size: params?.size ?? pageSize.value,
        name: params?.name ?? queryParams.name,
        num: params?.num ?? queryParams.num,
      })

      // 后端会把 current / size 纠正回合法值（比如删掉最后一页的数据后回退页码），
      // 所以整体以后端返回的分页对象为准，不要自己拼。
      page.value = res

      // 同步查询条件
      if (params?.name !== undefined) queryParams.name = params.name
      if (params?.num !== undefined) queryParams.num = params.num
    } finally {
      loading.value = false
    }
  }

  // 搜索（重置到第一页）
  const search = (params: { name?: string; num?: string }) => {
    return fetchList({ current: 1, ...params })
  }

  // 切换页码
  const changePage = (newPage: number, newSize?: number) => {
    return fetchList({ current: newPage, size: newSize })
  }

  /**
   * 直接绑给 Pagination 的 @change。
   * 换了每页条数一律回到第一页（和 usePageTable.onPageChange 的行为保持一致）。
   */
  const onPageChange = (payload: { current: number; size: number }) => {
    return fetchList({ current: payload.current, size: payload.size })
  }

  // 打开新增弹窗
  const openAdd = () => {
    isEdit.value = false
    /* Object.assign将源对象的属性复制到目标对象，覆盖同名属性，保持目标对象引用不变
       （重要，因为 formData 是响应式对象，直接替换会丢失响应性） */
    Object.assign(formData, emptyCollegeForm())
    formVisible.value = true
  }

  // 打开编辑弹窗
  const openEdit = async (id: number) => {
    try {
      const data = await getCollegeInfo(id)
      // College.id 是可选的，而 CollegeForm.id 是 number | null，
      // 这里显式归一化，否则数组下标式赋值会报类型错。
      Object.assign(formData, {
        id: data.id ?? id,
        name: data.name,
        num: data.num,
        tel: data.tel ?? '',
      })
      isEdit.value = true
      formVisible.value = true
    } catch {
      // 错误已由拦截器处理
    }
  }

  // 提交表单
  const submitForm = async () => {
    try {
      await saveCollege(formData)
      ElMessage.success(formData.id == null ? '新增成功' : '修改成功')
      formVisible.value = false
      await fetchList({ current: currentPage.value })
    } catch {
      // 错误已由拦截器处理
    }
  }

  // 删除
  const deleteItem = async (id: number) => {
    try {
      await ElMessageBox.confirm('确定删除该学院吗？删除后无法恢复。', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })

      await deleteCollege(id)
      ElMessage.success('删除成功')
      // 删的是当前页最后一条且不是第一页时往前退一页，避免停在空页上
      const target = list.value.length <= 1 && currentPage.value > 1
        ? currentPage.value - 1
        : currentPage.value
      await fetchList({ current: target })
    } catch (error) {
      if (error !== 'cancel') {
        // 其他错误
      }
    }
  }

  return {
    page,
    loading,
    queryParams,
    formData,
    formVisible,
    isEdit,
    total,
    currentPage,
    pageSize,
    list,
    hasPrevious,
    hasNext,
    fetchList,
    search,
    changePage,
    onPageChange,
    openAdd,
    openEdit,
    submitForm,
    deleteItem,
  }
})
