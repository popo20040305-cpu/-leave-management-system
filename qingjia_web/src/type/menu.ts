/**
 * 菜单/权限节点，对应后端 edu.hbmd.taotao.qingjia.pojo.MenuPojo（menu 表）。
 *
 * type: 1 目录、2 菜单、3 按钮（见后端 MenuType 常量）。
 * 目录和菜单有 url，按钮只有 permission，前端据此生成动态路由。
 */
export interface MenuPojo {
  id: number
  name: string
  permission: string
  /** 父节点 id，顶级目录为 0 */
  pid: number | null
  /** 父级 id 链，形如 '0,1,2,'，后端删子树时用它做 LIKE 匹配 */
  pids: string
  url: string | null
  type: number
  icon: string | null
  /**
   * 同级排序号。原来这里写成 order_mum（mum），和后端 JSON 里的 order_num 对不上，
   * 于是这个字段永远是 undefined。全项目只有这一处声明、没有读取方，所以改名不影响现有运行，
   * 但不改的话权限管理页拿不到排序号。
   */
  order_num: number
}

/** 带子节点的菜单，对应后端 vo.MenuVo（subMenus 递归嵌套） */
export interface MenuVo extends MenuPojo {
  subMenus: MenuVo[]
}
