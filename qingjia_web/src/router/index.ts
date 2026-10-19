import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '@/layout/MainLayout.vue'
import LoginViews from '../views/LoginViews.vue'
import { useMenuStore } from '@/stores/menuStore'
import { useBaseInfoStore } from '@/stores/baseInfoStore'
import { generateDynamicRoutes } from '@/utils/routeHelper'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: LoginViews,
  },
  {
    path: '/',
    name: 'main',
    component: MainLayout,
    /**
     * 默认子路由。原来 'main' 没有 children，登录成功后跳到 '/'，
     * MainLayout 渲染出来了但里面的 <router-view> 匹配不到任何组件，
     * 右侧内容区就是一片空白，必须手动点左侧菜单才有东西。
     * 这里把 HomeView 挂成 path: '' 的默认子路由，'/' 直接显示首页概览。
     *
     * 用懒加载而不是顶部静态 import：routeHelper 里的 import.meta.glob
     * 已经把 views 下所有组件收进去了，再静态引一次会让 Vite 报
     * INEFFECTIVE_DYNAMIC_IMPORT（LoginViews 现在就有这个警告）。
     */
    children: [
      {
        path: '',
        name: 'home',
        component: () => import('@/views/HomeView.vue'),
        meta: { title: '首页' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

router.beforeEach(async (to) => {
  if (to.path === '/login') {
    return true
  }

  const baseInfoStore = useBaseInfoStore()

  // 公共基础数据（学院/年级/性别/角色类型等字典），业务页的下拉框都靠它。
  // 1. 不阻塞导航：字典拉不到顶多是下拉框空着，不该把人踢回登录页。
  // 2. 放在 menuStore 判断之外，所以万一第一次没拉到，之后每次导航都会再试；
  //    store 内部有单飞去重，并发或重复调用只会发一个请求。
  if (!baseInfoStore.loaded) {
    void baseInfoStore.load().catch((error) => console.warn('基础数据加载失败:', error))
  }

  // 用 menuStore.loaded 当"动态路由是否已挂载"的标记，而不是模块级布尔量。
  // 这样退出登录时 menuStore.reset() 就能让路由在下次导航自动重建，
  // 换账号登录不会看到上一个账号的菜单。
  const menuStore = useMenuStore()
  if (!menuStore.loaded) {
    try {
      await menuStore.fetchMenu()

      // addRoute 的第一个参数是父路由的 name，不是 path。
      // 原来传 '/' 会报 [VUE_ROUTER_R0001] Parent route "/" not found，
      // 动态路由一条都挂不上去。这里挂到 name 为 'main' 的根路由下。
      // 生成的子路由都带 name，同名重复 addRoute 会覆盖旧的，不会累积重复路由。
      const children = generateDynamicRoutes(menuStore.menuVos)
      children.forEach((child) => router.addRoute('main', child))

      return to.fullPath
    } catch (error) {
      console.error('Failed to fetch menu list:', error)
      return '/login'
    }
  }
  return true
})
export default router
