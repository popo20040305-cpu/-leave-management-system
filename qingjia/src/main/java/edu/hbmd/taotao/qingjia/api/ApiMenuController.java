package edu.hbmd.taotao.qingjia.api;

import edu.hbmd.taotao.qingjia.constant.UserTypeConstant;
import edu.hbmd.taotao.qingjia.exception.ResultEnum;
import edu.hbmd.taotao.qingjia.exception.ResultException;
import edu.hbmd.taotao.qingjia.pojo.MenuPojo;
import edu.hbmd.taotao.qingjia.service.MenuService;
import edu.hbmd.taotao.qingjia.shiro.LoginUser;
import edu.hbmd.taotao.qingjia.util.R;
import edu.hbmd.taotao.qingjia.vo.MenuVo;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 菜单管理接口（Vue 前端使用），对应菜单「系统管理 -> 权限管理」(/menu/list)。
 *
 * /left.html 是主框架左侧菜单的数据源（所有登录用户都要能拿）；
 * 其余 list / save / info / delete / move 只给菜单管理页用，
 * 权限串必须和 menu 表里「权限管理」下的按钮权限一一对应：
 *   查询菜单 system:menu:query   添加菜单 system:menu:add
 *   修改菜单 system:menu:update  删除菜单 system:menu:delete
 * （上移下移复用「修改菜单」权限，menu 表里没有单独的权限项）
 *
 * URL 与老的 MenuController（Thymeleaf 那套）一一对应，只是加了 /api 前缀、
 * 改成返回 JSON，两套并存，老页面不受影响。
 */
@RestController
@RequestMapping("/api/menu")
public class ApiMenuController {

    private static final String PERM_QUERY = "system:menu:query";
    private static final String PERM_ADD = "system:menu:add";
    private static final String PERM_UPDATE = "system:menu:update";
    private static final String PERM_DELETE = "system:menu:delete";

    @Autowired
    MenuService menuService;

    /**
     * 主框架左侧菜单（按登录用户的角色过滤，返回目录 + 菜单两层）。
     * 这个接口不做 @RequiresPermissions：它就是登录后拿菜单用的，
     * 挂了注解反而会让「一个菜单都没分配」的用户拿不到任何东西。
     */
    @GetMapping("/left.html")
    public R left() {
        Subject subject = SecurityUtils.getSubject();
        LoginUser loginUser = (LoginUser) subject.getPrincipal();
        Integer userId;
        if (loginUser.getType() == UserTypeConstant.USER_TYPE_STUDENT) {
            userId = loginUser.getStudent().getId();
        } else {
            userId = loginUser.getTeacher().getId();
        }
        List<MenuVo> menuVos = menuService.getMenuVosByUserIdAndUserType(userId, loginUser.getType());
        Map<String, List<MenuVo>> map = new HashMap<>();
        map.put("menuVos", menuVos);
        return R.ok().put("data", map);
    }

    /**
     * 菜单管理页的完整菜单树（目录 -> 菜单 -> 按钮，三层）。
     * 与 /left.html 的区别：这里是给管理员维护用的，不做角色过滤。
     */
    @GetMapping("/list")
    @RequiresPermissions(PERM_QUERY)
    public R list() {
        List<MenuVo> menuTree = menuService.getTree();
        return R.ok().put("data", menuTree);
    }

    /**
     * 保存菜单（新增或修改，靠 id 是否为空区分）。
     *
     * 与 ApiCollegeController.save 同样的理由不能用 @RequiresPermissions 静态挂
     * add / update：一个方法管两件事，只挂 add 等于「有新增权限的人也能改数据」。
     * 这里按 id 动态校验。
     */
    @PostMapping("/save")
    public R save(@Valid @RequestBody MenuPojo menu) {
        if (menu == null) {
            return R.error(400, "请求体不能为空");
        }
        boolean isUpdate = menu.getId() != null;
        SecurityUtils.getSubject().checkPermission(isUpdate ? PERM_UPDATE : PERM_ADD);

        if (menu.getPid() == null) {
            menu.setPid(0);
        }
        if (menu.getPid().equals(menu.getId())) {
            return R.error("父节点不能是自己");
        }
        // 修改时校验父节点存在，且不能把节点挂到自己的子树下（会形成环）
        if (menu.getPid() != 0) {
            MenuPojo parent = menuService.getById(menu.getPid());
            if (parent == null) {
                throw new ResultException(ResultEnum.MENU_NOT_EXIST);
            }
            if (isUpdate) {
                String parentPids = parent.getPids() == null ? "" : parent.getPids();
                if (parentPids.contains("," + menu.getId() + ",") || parent.getId().equals(menu.getId())) {
                    return R.error("不能把菜单移动到它自己的子菜单下");
                }
            }
        }
        // 目录 / 菜单才需要 url，按钮清空 url，避免脏数据生成无效路由
        if (menu.getType() != null && menu.getType() == 3) {
            menu.setUrl(null);
        }

        try {
            boolean success = menuService.saveMenu(menu);
            if (success) {
                return R.ok("保存成功").put("data", menu);
            } else {
                return R.error("保存失败");
            }
        } catch (Exception e) {
            return R.error(e.getMessage());
        }
    }

    /**
     * 单个菜单详情（编辑回显）。
     */
    @GetMapping("/info/{id}")
    @RequiresPermissions(PERM_QUERY)
    public R info(@PathVariable Integer id) {
        MenuPojo menu = menuService.getById(id);
        if (menu == null) {
            throw new ResultException(ResultEnum.MENU_NOT_EXIST);
        }
        return R.ok().put("data", menu);
    }

    /**
     * 删除菜单（级联删除所有子孙）。
     */
    @DeleteMapping("/delete/{id}")
    @RequiresPermissions(PERM_DELETE)
    public R delete(@PathVariable Integer id) {
        if (menuService.getById(id) == null) {
            throw new ResultException(ResultEnum.MENU_NOT_EXIST);
        }
        boolean success = menuService.deleteMenu(id);
        if (success) {
            return R.ok("删除成功");
        } else {
            return R.error("删除失败");
        }
    }

    /**
     * 同级上移 / 下移。复用「修改菜单」权限，menu 表里没有单独的移动权限。
     */
    @PostMapping("/move")
    @RequiresPermissions(PERM_UPDATE)
    public R move(@RequestParam Integer id, @RequestParam String direction) {
        try {
            boolean success = menuService.moveMenu(id, direction);
            if (success) {
                return R.ok("移动成功");
            } else {
                return R.error("移动失败");
            }
        } catch (ResultException e) {
            return R.error(e.getStatus(), e.getMessage());
        } catch (Exception e) {
            return R.error(e.getMessage());
        }
    }
}
