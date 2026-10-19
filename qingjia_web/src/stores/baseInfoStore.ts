import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { getBaseInfoApi, getBaseInfoByKeysApi } from '@/api/BaseInfoApi'
import { emptyBaseInfo, type BaseInfoData, type BaseInfoKey } from '@/type/BaseInfoType'

/**
 * 公共基础数据（字典）store。
 *
 * 三个要点：
 * 1. 单飞（single-flight）：应用启动时多个组件同时 await load()，只发一个请求。
 * 2. 学院列表后端是按登录用户过滤的（学生只看本班所属学院、教师只看本学院），
 *    所以换账号必须 reset()，不能沿用上一个账号的数据。
 * 3. 某个字典被增删改之后走 reload(['colleges']) 局部刷新，不用整包重拉。
 */
export const useBaseInfoStore = defineStore('baseInfo', () => {
  const data = ref<BaseInfoData>(emptyBaseInfo())
  const loaded = ref(false)
  const loading = ref(false)

  // 正在进行中的请求，用于并发去重
  let pending: Promise<void> | null = null

  /**
   * 加载全部字典。已加载过则直接返回，除非 force=true。
   * 失败时把异常抛给调用方，由调用方决定是拦下来还是只打个 warning。
   */
  const load = async (force = false): Promise<void> => {
    if (loaded.value && !force) return
    if (pending) return pending

    loading.value = true
    pending = (async () => {
      try {
        data.value = await getBaseInfoApi()
        loaded.value = true
      } finally {
        pending = null
        loading.value = false
      }
    })()
    return pending
  }

  /**
   * 刷新字典。传 keys 只刷新这几个（增删改学院/角色之后用），
   * 不传则全量重拉。
   */
  const reload = async (keys?: BaseInfoKey[]): Promise<void> => {
    if (!keys || keys.length === 0) {
      await load(true)
      return
    }
    const patch = await getBaseInfoByKeysApi(keys)
    data.value = { ...data.value, ...patch }
    loaded.value = true
  }

  /** 退出登录 / 修改密码后调用 */
  const reset = () => {
    data.value = emptyBaseInfo()
    loaded.value = false
    loading.value = false
    pending = null
  }

  /**
   * 按 id 取字典的显示名。下拉框回显、表格列格式化都用它，
   * 省得每个页面自己写一遍 find。
   *
   * 兼容三种字段名：多数字典用 name，年级用 grade，
   * 申请状态用 applicationStutas（后端就是这么拼的，见 BaseInfoType.ts）。
   */
  const labelOf = (key: BaseInfoKey, id: number | null | undefined): string => {
    if (id === null || id === undefined) return ''
    const list = data.value[key] as Array<Record<string, unknown>>
    const hit = list.find((item) => item.id === id)
    if (!hit) return ''
    const label = hit.name ?? hit.grade ?? hit.applicationStutas
    return label === null || label === undefined ? String(id) : String(label)
  }

  return {
    data,
    loaded,
    loading,
    load,
    reload,
    reset,
    labelOf,
    // 常用字典单独暴露成 computed，模板里可以直接 baseInfoStore.colleges 遍历
    colleges: computed(() => data.value.colleges),
    grades: computed(() => data.value.grades),
    aproveTypes: computed(() => data.value.aproveTypes),
    leaveApplicationTypes: computed(() => data.value.leaveApplicationTypes),
    menuTypes: computed(() => data.value.menuTypes),
    roleTypes: computed(() => data.value.roleTypes),
    sexTypes: computed(() => data.value.sexTypes),
    applicationStatuses: computed(() => data.value.applicationStatuses),
  }
})
