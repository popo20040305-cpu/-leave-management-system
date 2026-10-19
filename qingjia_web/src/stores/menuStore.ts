import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { MenuPojo, MenuVo } from '@/type/menu'
import {
  deleteMenu,
  getMenuInfo,
  getMenuList,
  getMenuTree,
  moveMenu,
  saveMenu,
  type MenuSaveForm,
} from '@/api/menuApi'

/**
 * 菜单 store，管两摊事：
 *
 * A. 主框架左侧菜单（menuVos / loaded / fetchMenu / reset）
 *    路由守卫靠 loaded 判断动态路由是否已挂载，退出登录时调 reset() 重建，
 *    这部分是既有逻辑，任何改动都要保证 router/index.ts 和 MainLayout 不受影响。
 *
 * B. 「系统管理 -> 权限管理」页面的管理态（menuTree / currentMenu / editMode 及其动作）。
 *    和 A 用的是不同接口（/api/menu/list 是完整树，/api/menu/left.html 是按角色过滤的），
 *    数据故意不混用：管理页要是只显示当前用户有权的菜单，就没法维护别人的权限了。
 */
export const useMenuStore = defineStore('menu', () => {
  /* ---------------- A. 左侧菜单（框架用） ---------------- */

  const menuVos = ref<MenuVo[]>([])
  const loaded = ref(false)

  const fetchMenu = async () => {
    if (!loaded.value) {
      try {
        const res = await getMenuList()
        menuVos.value = res.menuVos
        loaded.value = true
      } catch (err) {
        console.error('获取菜单失败，error:', err)
        // 必须往外抛：路由守卫靠这个 reject 才能判断"没登录/token 过期"
        // 并跳去 /login。原来吞掉异常，守卫会继续往下走，
        // 用空的 menuVos 生成 0 条路由，页面就白屏停在 '/'。
        throw err
      }
    }
  }

  /**
   * 退出登录 / 改密后调用：清掉菜单并让路由守卫重新拉取。
   * 不重置的话，换一个账号登录看到的还是上一个账号的菜单和动态路由。
   */
  const reset = () => {
    menuVos.value = []
    loaded.value = false
    // 顺带把管理页状态也清掉，避免 A 账号改菜单改到一半，
    // B 账号登录后右侧还挂着 A 留下的编辑面板
    resetCurrent()
    menuTree.value = []
  }

  /* ---------------- B. 菜单管理页 ---------------- */

  /** 完整菜单树（目录 -> 菜单 -> 按钮） */
  const menuTree = ref<MenuVo[]>([])
  const treeLoading = ref(false)

  /** 右侧正在编辑的菜单。null 表示没选中，右侧显示占位提示 */
  const currentMenu = ref<MenuPojo | null>(null)
  /** 'add' 新增（提交不带 id）/ 'edit' 修改（提交带 id） */
  const editMode = ref<'add' | 'edit'>('add')

  /** 有新节点保存成功后，树要重新拉一次并展开，这里记下待定位的节点 id */
  const pendingFocusId = ref<number | null>(null)

  const fetchTree = async () => {
    treeLoading.value = true
    try {
      menuTree.value = await getMenuTree()
    } catch (err) {
      console.error('获取菜单树失败:', err)
      // 列表加载失败不该让整页崩掉，错误提示拦截器已经弹过了
      menuTree.value = []
      throw err
    } finally {
      treeLoading.value = false
    }
  }

  /** 点树节点：拉详情回显（列表可能是旧的，详情接口同时验证了 query 权限） */
  const getDetail = async (id: number) => {
    try {
      const detail = await getMenuInfo(id)
      currentMenu.value = detail
      editMode.value = 'edit'
    } catch {
      // 拉不到就保持现状，错误提示拦截器已弹过
    }
  }

  /** 新增：pid 挂到指定父节点，type 由页面按父节点类型决定 */
  const createNew = (pid: number, type: number) => {
    currentMenu.value = {
      id: 0, // 0 表示新建，提交时后端按「id 为空或 0」当新增处理
      name: '',
      permission: '',
      pid,
      pids: '',
      url: '',
      type,
      icon: '',
      order_num: 0,
    }
    editMode.value = 'add'
  }

  const resetCurrent = () => {
    currentMenu.value = null
    editMode.value = 'add'
  }

  /** 保存（新增 / 修改共用）。成功后刷新树，新增的节点会被定位到 */
  const save = async (data: MenuPojo) => {
    // 后端靠「id 是否为 null」区分新增和修改，0 是前端占位值，必须抹掉
    const payload: MenuSaveForm = { ...data, id: data.id === 0 ? null : data.id }
    const saved = await saveMenu(payload)
    await fetchTree()
    if (saved?.id) {
      pendingFocusId.value = saved.id
      currentMenu.value = saved
      editMode.value = 'edit'
    }
    return saved
  }

  /** 删除。删的是当前正在编辑的节点时，顺带清空右侧编辑面板 */
  const remove = async (id: number) => {
    await deleteMenu(id)
    if (currentMenu.value?.id === id) {
      resetCurrent()
    }
    await fetchTree()
  }

  /** 同级上移 / 下移。成功后重拉树，保持选中节点不变 */
  const move = async (id: number, direction: 'up' | 'down') => {
    await moveMenu(id, direction)
    await fetchTree()
  }

  /** 树刷新后由页面调用：取走待定位的节点 id（取一次即清空），用于滚动高亮 */
  const takePendingFocus = (): number | null => {
    const id = pendingFocusId.value
    pendingFocusId.value = null
    return id
  }

  return {
    // A
    menuVos,
    loaded,
    fetchMenu,
    reset,
    // B
    menuTree,
    treeLoading,
    currentMenu,
    editMode,
    fetchTree,
    getDetail,
    createNew,
    resetCurrent,
    save,
    remove,
    move,
    takePendingFocus,
  }
})
