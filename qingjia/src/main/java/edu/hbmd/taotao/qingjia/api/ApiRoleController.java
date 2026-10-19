package edu.hbmd.taotao.qingjia.api;

import edu.hbmd.taotao.qingjia.controller.BaseController;
import edu.hbmd.taotao.qingjia.exception.ResultEnum;
import edu.hbmd.taotao.qingjia.exception.ResultException;
import edu.hbmd.taotao.qingjia.pojo.RolePojo;
import edu.hbmd.taotao.qingjia.service.MenuService;
import edu.hbmd.taotao.qingjia.service.RoleMenuService;
import edu.hbmd.taotao.qingjia.service.RoleService;
import edu.hbmd.taotao.qingjia.util.R;
import edu.hbmd.taotao.qingjia.vo.MenuVo;
import edu.hbmd.taotao.qingjia.vo.PageResult;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 角色管理接口（Vue 前端使用），对应菜单「系统管理 -> 角色管理」(/role/index)。
 *
 * 权限串与 menu 表一致：
 *   查询角色 system:role:query        添加角色 system:role:add
 *   修改角色 system:role:update       删除角色 system:role:delete
 *   指定角色权限 system:role:zdjsqx
 *
 * 与老 RoleController（Thymeleaf 那套）的区别，和 ApiMenuController 一样：
 * 加 /api 前缀、/index 返回 PageResult JSON 而不是视图名；
 * permTree 额外剔除父节点 id（原因见 permTree 方法注释）。
 *
 * 继承 BaseController 只是为了复用 getPageSize()（按登录用户记忆每页条数），
 * 本类用不到它注入的 ServletContext / 各类 Service。
 */
@RestController
@RequestMapping("/api/role")
public class ApiRoleController extends BaseController {

    private static final String PERM_QUERY = "system:role:query";
    private static final String PERM_ADD = "system:role:add";
    private static final String PERM_UPDATE = "system:role:update";
    private static final String PERM_DELETE = "system:role:delete";
    private static final String PERM_SAVE_PERM = "system:role:zdjsqx";

    private static final int MAX_PAGE_SIZE = 200;

    @Autowired
    private RoleService roleService;
    @Autowired
    private MenuService menuService;
    @Autowired
    private RoleMenuService roleMenuService;

    /**
     * 角色分页列表。参数名与前端 usePageTable 对齐（current / size / name），
     * 返回体统一用 PageResult，理由同 ApiCollegeController。
     */
    @GetMapping("/index")
    @RequiresPermissions(PERM_QUERY)
    public R index(@RequestParam(value = "current", defaultValue = "1") long current,
                   @RequestParam(value = "size", required = false) Long size,
                   @RequestParam(value = "name", required = false) String name) {
        int safeCurrent = current < 1 ? 1 : (int) Math.min(current, Integer.MAX_VALUE);
        // size 不传时用登录用户在 session 里记住的每页条数（没设置过则默认 10）
        long effectiveSize = (size == null || size < 1)
                ? getPageSize()
                : Math.min(size, MAX_PAGE_SIZE);
        return R.ok().put("data", PageResult.of(
                roleService.pageRoles(safeCurrent, (int) effectiveSize, trimToNull(name))));
    }

    /**
     * 新增 / 修改角色。与 ApiMenuController.save 同样的理由按 id 动态校验权限。
     */
    @PostMapping("/save")
    public R save(@Valid @RequestBody RolePojo role) {
        if (role == null) {
            return R.error(400, "请求体不能为空");
        }
        boolean isUpdate = role.getId() != null;
        SecurityUtils.getSubject().checkPermission(isUpdate ? PERM_UPDATE : PERM_ADD);

        role.setName(trimToNull(role.getName()));
        role.setRemark(trimToNull(role.getRemark()));
        try {
            boolean success = roleService.saveOrUpdateRole(role);
            if (success) {
                return R.ok("操作成功").put("data", role);
            }
            return R.error("角色名称已存在或操作失败");
        } catch (ResultException e) {
            return R.error(e.getStatus(), e.getMessage());
        }
    }

    /**
     * 角色详情（编辑回显）。
     */
    @GetMapping("/info/{id}")
    @RequiresPermissions(PERM_QUERY)
    public R info(@PathVariable Integer id) {
        RolePojo role = roleService.getRoleById(id);
        if (role == null) {
            throw new ResultException(ResultEnum.ROLE_NOT_EXIST);
        }
        return R.ok().put("data", role);
    }

    /**
     * 删除角色（连带删除 role_menu / user_role 关联）。
     */
    @DeleteMapping("/delete/{id}")
    @RequiresPermissions(PERM_DELETE)
    public R delete(@PathVariable Integer id) {
        if (roleService.getRoleById(id) == null) {
            throw new ResultException(ResultEnum.ROLE_NOT_EXIST);
        }
        return roleService.deleteRoleAndRelations(id)
                ? R.ok("删除成功")
                : R.error("删除失败");
    }

    /**
     * 角色权限树：完整菜单树 + 该角色已勾选的叶子权限 id。
     *
     * checkedIds 必须剔除父节点：element-plus 的 el-tree 勾选父节点会自动勾中整棵子树，
     * 如果把「目录 / 菜单」这些父节点 id 也回显成选中状态，打开弹窗时整棵树会被全部勾上，
     * 用户只要一点保存，角色就把所有权限都拿走了。所以回显时只留叶子（真正带权限串的节点），
     * 父节点是否选中由勾选联动（check-strictly=false）自动推导。
     *
     * 保存时则相反（见 savePerm）：勾了子节点必须把父节点也存进去，
     * 否则左侧菜单里连目录都没有，页面根本渲染不出来。
     */
    @GetMapping("/permTree/{roleId}")
    @RequiresPermissions(value = {PERM_QUERY, "system:menu:query"}, logical = Logical.AND)
    public R permTree(@PathVariable Integer roleId) {
        if (roleService.getRoleById(roleId) == null) {
            throw new ResultException(ResultEnum.ROLE_NOT_EXIST);
        }
        List<MenuVo> allMenus = menuService.getTree();
        List<Integer> roleIds = new ArrayList<>();
        roleIds.add(roleId);
        Set<Integer> checkedIds = roleMenuService.getMenuIdsByRoleIds(roleIds);
        // 剔除父节点，避免回显时整棵树被勾满
        Set<Integer> parentIds = new HashSet<>();
        collectParentIds(allMenus, parentIds);
        checkedIds.removeAll(parentIds);

        Map<String, Object> data = new HashMap<>();
        data.put("tree", allMenus);
        data.put("checkedIds", new ArrayList<>(checkedIds));
        return R.ok().put("data", data);
    }

    /**
     * 递归收集所有「有子节点」的父节点 id。
     */
    private void collectParentIds(List<MenuVo> menus, Set<Integer> parentIds) {
        if (menus == null || menus.isEmpty()) {
            return;
        }
        for (MenuVo menu : menus) {
            if (menu.getSubMenus() != null && !menu.getSubMenus().isEmpty()) {
                parentIds.add(menu.getId());
                collectParentIds(menu.getSubMenus(), parentIds);
            }
        }
    }

    /**
     * 保存角色权限。menuIds 只含前端勾选的叶子节点，
     * 父节点的补全交给 roleMenuService.saveRoleMenus（见那边注释）。
     */
    @PostMapping("/savePerm")
    @RequiresPermissions(PERM_SAVE_PERM)
    public R savePerm(@RequestParam Integer roleId,
                      @RequestParam(value = "menuIds", required = false) List<Integer> menuIds) {
        if (roleService.getRoleById(roleId) == null) {
            throw new ResultException(ResultEnum.ROLE_NOT_EXIST);
        }
        boolean success = roleMenuService.saveRoleMenus(roleId, menuIds);
        return success ? R.ok("权限保存成功") : R.error("权限保存失败");
    }

    /**
     * 全部角色（不分页）。教师指定角色、请假模块按角色判断数据范围时用。
     */
    @GetMapping("/listAll")
    @RequiresPermissions(PERM_QUERY)
    public R listAll() {
        return R.ok().put("data", roleService.list());
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
