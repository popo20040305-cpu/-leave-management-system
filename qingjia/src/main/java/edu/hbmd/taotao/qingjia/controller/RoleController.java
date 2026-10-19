package edu.hbmd.taotao.qingjia.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import edu.hbmd.taotao.qingjia.constant.PageConstant;
import edu.hbmd.taotao.qingjia.pojo.RolePojo;
import edu.hbmd.taotao.qingjia.service.MenuService;
import edu.hbmd.taotao.qingjia.service.RoleMenuService;
import edu.hbmd.taotao.qingjia.service.RoleService;
import edu.hbmd.taotao.qingjia.service.UserRoleService;
import edu.hbmd.taotao.qingjia.util.R;
import edu.hbmd.taotao.qingjia.vo.MenuVo;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/role")
public class RoleController extends BaseController {
    @Autowired
    UserRoleService userRoleService;

    @Autowired
    RoleService roleService;

    @Autowired
    MenuService menuService;

    @Autowired
    RoleMenuService roleMenuService;


    @GetMapping("/getRoleIds/{userId}/{userType}")
    @ResponseBody
    public R getRoleIdsByUserIdAndUserType(@PathVariable Integer userId, @PathVariable Integer userType){
        Set<String> roleNames= roleService.getRoleNamesByUserIdAndUserType(userId,userType);

        return R.ok().put("roleNames",roleNames);
    }

    /**
     * 角色列表页面（服务端渲染）
     * 必须包含 curPage 参数，支持 name 查询
     */
    @GetMapping("/index")
    @RequiresPermissions("system:role:query")
    public String index(@RequestParam(defaultValue = "1") Integer curPage,
                        @RequestParam(required = false) String name,
                        Model model) {

        IPage<RolePojo> page = roleService.pageRoles(curPage, PageConstant.pageSize, name);
        model.addAttribute("page", page);
        //model.addAttribute("name", name);
        model.addAttribute("roleTypes", getRoleTypes()); // 从 ServletContext 获取角色类型
        return "role/role";
    }
    // ========== AJAX 接口 ==========
    @PostMapping("/save")
    @ResponseBody
    @RequiresPermissions(value = {"system:role:add","system:role:update"},logical = Logical.OR)
    public R save(@Valid  @RequestBody RolePojo role) {
        boolean success = roleService.saveOrUpdateRole(role);
        if (success) {
            return R.ok("操作成功");
        } else {
            return R.error("角色名称已存在或操作失败");
        }
    }
    @GetMapping("/info/{id}")
    @ResponseBody
    @RequiresPermissions("system:role:query")
    public R info(@PathVariable Integer id) {
        RolePojo role = roleService.getRoleById(id);
        if (role != null) {
            return R.ok().put("data", role);
        } else {
            return R.error("角色不存在");
        }
    }
    @DeleteMapping("/delete/{id}")
    @ResponseBody
    @RequiresPermissions("system:role:delete")
    public R delete(@PathVariable Integer id) {
        boolean success = roleService.deleteRoleAndRelations(id);
        return success ? R.ok("删除成功") : R.error("删除失败");
    }
    /**
     * 获取角色权限树（树形菜单 + 已选中的菜单ID）
     */
    @GetMapping("/permTree/{roleId}")
    @ResponseBody
    @RequiresPermissions(value = {"system:role:query","system:menu:query"},logical = Logical.AND)
    public R permTree(@PathVariable Integer roleId) {
        List<MenuVo> allMenus = menuService.getTree();
        List<Integer> roleIds = new ArrayList<>();
        roleIds.add(roleId);
        Set<Integer> checkedIds = roleMenuService.getMenuIdsByRoleIds(roleIds);
        return R.ok().put("tree", allMenus).put("checkedIds", checkedIds);
    }
    @PostMapping("/savePerm")
    @ResponseBody
    @RequiresPermissions("system:role:zdjsqx")
    public R savePerm(@RequestParam Integer roleId, @RequestParam(required = false) List<Integer> menuIds) {
        boolean success = roleMenuService.saveRoleMenus(roleId, menuIds);
        return success ? R.ok("权限保存成功") : R.error("权限保存失败");
    }

    @GetMapping("/listAll")
    @ResponseBody
    @RequiresPermissions("system:role:query")
    public R listAll() {
        List<RolePojo> list = roleService.list();
        return R.ok().put("data", list);
    }
}
