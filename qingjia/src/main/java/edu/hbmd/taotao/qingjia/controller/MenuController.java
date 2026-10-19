package edu.hbmd.taotao.qingjia.controller;

import edu.hbmd.taotao.qingjia.pojo.MenuPojo;
import edu.hbmd.taotao.qingjia.service.MenuService;
import edu.hbmd.taotao.qingjia.service.RoleMenuService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/menu")
public class MenuController {
    @Autowired
    UserRoleService userRoleService;
    @Autowired
    RoleMenuService roleMenuService;

    @Autowired
    MenuService menuService;

    /**
     * 菜单管理主页面（Thymeleaf 渲染）
     * 访问路径：/menu/list 或 /menu
     */
    @GetMapping("/list")
    @RequiresPermissions("system:menu:query")
    public String list(Model model) {
        // 获取完整的菜单树（目录→菜单→按钮）
        List<MenuVo> menuTree = menuService.getTree();
        model.addAttribute("menuTree", menuTree);
        return "menu/menu";   // 对应 templates/menu/menu.html
    }
    /**
     * AJAX: 保存或更新菜单
     */
    @PostMapping("/save")
    @ResponseBody
    @RequiresPermissions(value = {"system:menu:add","system:menu:update"},logical = Logical.OR)
    public R  saveMenu(@Valid  @RequestBody MenuPojo menu) {
        try {
            boolean success = menuService.saveMenu(menu);
            if (success){
                return R.ok("保存成功");
            }else {
                return R.error("保存失败");
            }
        } catch (Exception e) {
            return R.error(e.getMessage());
        }
    }
    /**
     * AJAX: 获取单个菜单详情（用于编辑回显）
     */
    @GetMapping("/info/{id}")
    @ResponseBody
    @RequiresPermissions("system:menu:query")
    public R getMenuInfo(@PathVariable Integer id) {
        try {
            MenuPojo menu = menuService.getById(id);
            if (menu != null) {
                return R.ok().put("data", menu);
            } else {
                return R.error("菜单不存在");
            }
        } catch (Exception e) {
            return R.error(e.getMessage());
        }
    }
    /**
     * AJAX: 删除菜单
     */
    @DeleteMapping("/delete/{id}")
    @ResponseBody
    @RequiresPermissions("system:menu:delete")
    public R deleteMenu(@PathVariable Integer id) {
        try {
            boolean success = menuService.deleteMenu(id);
            if (success){
                return R.ok("删除成功");
            }else {
                return R.error("删除失败");
            }
        } catch (Exception e) {
            return R.error(e.getMessage());
        }
    }
}
