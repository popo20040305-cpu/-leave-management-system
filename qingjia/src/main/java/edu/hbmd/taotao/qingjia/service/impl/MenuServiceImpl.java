package edu.hbmd.taotao.qingjia.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.hbmd.taotao.qingjia.constant.MenuType;
import edu.hbmd.taotao.qingjia.constant.UserTypeConstant;
import edu.hbmd.taotao.qingjia.exception.ResultEnum;
import edu.hbmd.taotao.qingjia.exception.ResultException;
import edu.hbmd.taotao.qingjia.mapper.MenuMapper;
import edu.hbmd.taotao.qingjia.pojo.MenuPojo;
import edu.hbmd.taotao.qingjia.pojo.RolePojo;
import edu.hbmd.taotao.qingjia.service.MenuService;
import edu.hbmd.taotao.qingjia.service.RoleMenuService;
import edu.hbmd.taotao.qingjia.service.RoleService;
import edu.hbmd.taotao.qingjia.service.UserRoleService;
import edu.hbmd.taotao.qingjia.shiro.ShiroProperties;
import edu.hbmd.taotao.qingjia.vo.MenuVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MenuServiceImpl extends ServiceImpl<MenuMapper, MenuPojo>
implements MenuService {
    @Autowired
    UserRoleService userRoleService;
    @Autowired
    RoleMenuService roleMenuService;
    @Autowired
    RoleService roleService;
    @Autowired
    ShiroProperties shiroProperties;
    @Override
    public List<MenuPojo> getMenusByIds(Set<Integer> ids) {
        QueryWrapper<MenuPojo> wrapper = new QueryWrapper<>();
        wrapper.in("id", ids)
                .orderByAsc("pid", "order_num");
        return baseMapper.selectBatchIds(ids);
    }

    @Override
    public Set<String> getMenuNamesByUserIdAndUserType(Integer userId, Integer userType) {
        List<Integer> roleIds = userRoleService.getRoleIdsByUserIdAndUserType(userId, userType);
        if (roleIds==null){
            roleIds = new ArrayList<>();
        }
        LambdaQueryWrapper<RolePojo> wrapper = new LambdaQueryWrapper<>();
        if (userType == UserTypeConstant.USER_TYPE_STUDENT){
            wrapper.eq(RolePojo::getName,shiroProperties.getStudentDefaultRoleName());
        }else{
            wrapper.eq(RolePojo::getName,shiroProperties.getTeacherDefaultRoleName());
        }
        RolePojo rolePojo = roleService.getOne(wrapper);
        if (rolePojo!=null){
            roleIds.add(rolePojo.getId());
        }
        Set<Integer> menuIds = roleMenuService.getMenuIdsByRoleIds(roleIds);
        List<MenuPojo> menus = this.getMenusByIds(menuIds);
        Set<String> menuNames = new HashSet<>();
        for (MenuPojo menuPojo : menus) {
            menuNames.add(menuPojo.getPermission());
        }
        return menuNames;
    }

    @Override
    public List<MenuVo> getMenuVosByUserIdAndUserType(Integer userId, Integer userType) {
        List<Integer> roleIds = userRoleService.getRoleIdsByUserIdAndUserType(userId, userType);
        if (roleIds==null){
            roleIds = new ArrayList<>();
        }
        LambdaQueryWrapper<RolePojo> wrapper = new LambdaQueryWrapper<>();
        if (userType == UserTypeConstant.USER_TYPE_STUDENT){
            wrapper.eq(RolePojo::getName,shiroProperties.getStudentDefaultRoleName());
        }else{
            wrapper.eq(RolePojo::getName,shiroProperties.getTeacherDefaultRoleName());
        }
        RolePojo rolePojo = roleService.getOne(wrapper);
        if (rolePojo != null){
            roleIds.add(rolePojo.getId());
        }
        Set<Integer> menuIds = roleMenuService.getMenuIdsByRoleIds(roleIds);
        List<MenuPojo> menus = this.getMenusByIds(menuIds);
        List<MenuVo> menuVos = new ArrayList<>();
        for (MenuPojo menuPojo : menus) {
            if (menuPojo.getPid() == 0) {
                MenuVo menuVo = new MenuVo(menuPojo);
                for (MenuPojo menu : menus) {
                    if (menu.getPid() == menuPojo.getId()) {
                        menuVo.addSubMenu(menu);
                    }
                }
                menuVos.add(menuVo);
            }
        }
        return menuVos;
    }

    @Override
    public List<MenuVo> getTree() {

        List<MenuPojo> all = this.list(new QueryWrapper<MenuPojo>()
                .orderByAsc("order_num"));
        // 构建树：先找顶级目录（pid=0）
        return buildTree(all);

    }

    @Override
    @Transactional
    public boolean saveMenu(MenuPojo menu) {
        // 生成 pids 字段（父级id链）
        if (menu.getPid() == null || menu.getPid() == 0) {
            menu.setPids("0,");
        } else {
            MenuPojo parent = this.getById(menu.getPid());
            if (parent != null && parent.getPids() != null) {
                menu.setPids(parent.getPids() + menu.getPid() + ",");
            } else {
                menu.setPids("0," + menu.getPid() + ",");
            }
        }
        boolean result;
        if (menu.getId() == null) {
            // 新增时前端一般不指定排序号（传 0 或不传）。让它排到同级末尾：
            // 取同 pid 下最大的 order_num + 1。不处理的话 0 会被下面的
            // reBuildOrderNum 归一化成 1，新菜单永远插在同级第一个，
            // 每加一个就把老顺序搅一遍。
            if (menu.getOrder_num() == null || menu.getOrder_num() <= 0) {
                List<MenuPojo> siblings = this.list(new QueryWrapper<MenuPojo>()
                        .eq("pid", menu.getPid() == null ? 0 : menu.getPid()));
                int max = 0;
                for (MenuPojo sibling : siblings) {
                    if (sibling.getOrder_num() != null && sibling.getOrder_num() > max) {
                        max = sibling.getOrder_num();
                    }
                }
                menu.setOrder_num(max + 1);
            }
            result = this.save(menu);
        } else {
            result = this.updateById(menu);
        }
        reBuildOrderNum(menu.getPid());
        return result;
    }

    @Override
    @Transactional
    public boolean deleteMenu(Integer id) {
        // 删除自身及所有子孙（根据 pids 包含该 id 的）
        List<MenuPojo> toDelete = this.list(new QueryWrapper<MenuPojo>()
                .apply("CONCAT(',', pids, ',') LIKE CONCAT('%,', {0}, ',%')", id));
        Set<Integer> ids = new HashSet<>();
        for (MenuPojo menuPojo : toDelete) {
            ids.add(menuPojo.getId());
        }
        ids.add(id);
        return this.removeByIds(ids);
    }

    /**
     * 同级移动菜单（上移 / 下移）。
     *
     * 做法：取出同 pid 的兄弟节点按 order_num 升序排列，找到当前节点的位置，
     * 和相邻节点交换 order_num 后批量更新。文档 13.1 的实现即此思路；
     * 这里先 reBuildOrderNum 归一排序号，避免库里存在 order_num 重复 / 为 NULL 时
     * 交换后顺序不变的边界问题。
     */
    @Override
    @Transactional
    public boolean moveMenu(Integer id, String direction) {
        MenuPojo current = this.getById(id);
        if (current == null) {
            throw new ResultException(ResultEnum.MENU_NOT_EXIST);
        }
        Integer pid = current.getPid();
        if (pid == null) {
            pid = 0;
        }
        // 先把同级排序号归一为 1..n，保证下面的相邻交换总是有效
        reBuildOrderNum(pid);

        List<MenuPojo> siblings = this.list(new QueryWrapper<MenuPojo>()
                .eq("pid", pid)
                .orderByAsc("order_num"));
        int index = -1;
        for (int i = 0; i < siblings.size(); i++) {
            if (siblings.get(i).getId().equals(id)) {
                index = i;
                break;
            }
        }
        if (index == -1) {
            throw new ResultException(ResultEnum.MENU_NOT_EXIST);
        }

        int targetIndex;
        if ("up".equalsIgnoreCase(direction)) {
            if (index == 0) {
                throw new ResultException(ResultEnum.ERROR.getStatus(), "已经是第一个，不能上移");
            }
            targetIndex = index - 1;
        } else if ("down".equalsIgnoreCase(direction)) {
            if (index == siblings.size() - 1) {
                throw new ResultException(ResultEnum.ERROR.getStatus(), "已经是最后一个，不能下移");
            }
            targetIndex = index + 1;
        } else {
            throw new ResultException(ResultEnum.ERROR.getStatus(), "移动方向只能是 up 或 down");
        }

        MenuPojo target = siblings.get(targetIndex);
        Integer tmp = current.getOrder_num();
        current.setOrder_num(target.getOrder_num());
        target.setOrder_num(tmp);
        List<MenuPojo> toUpdate = new ArrayList<>();
        toUpdate.add(current);
        toUpdate.add(target);
        return this.updateBatchById(toUpdate);
    }

    private void reBuildOrderNum(Integer pid) {
        QueryWrapper<MenuPojo> queryWrapper = new QueryWrapper();
        queryWrapper.eq("pid",pid);
        queryWrapper.orderByAsc("order_num");
        List<MenuPojo> menuPojos = baseMapper.selectList(queryWrapper);
        boolean needUpdate = false;
        Integer expected = 1;
        for (MenuPojo menu : menuPojos) {
            Integer currentOrder = menu.getOrder_num();
            if (currentOrder != expected) {
                menu.setOrder_num(expected);
                needUpdate = true;
            }
            expected++;
        }
        if (needUpdate) {
            // 批量更新（每个菜单单独更新，或者使用 updateBatchById）
            this.updateBatchById(menuPojos);
        }

    }

    private List<MenuVo> buildTree(List<MenuPojo> all) {
        List<MenuVo> tree = new ArrayList<>();
        for (MenuPojo dir:all){
            if (dir.getType() == MenuType.MENU_TYPE_DIR){
                MenuVo menuVo = new MenuVo(dir);
                tree.add(menuVo);
            }
        }
        for (MenuVo dir:tree){
            for (MenuPojo menu:all){
                if (menu.getPid() == dir.getId()){
                    dir.addSubMenu(menu);
                }
            }
        }
        for (MenuVo dir:tree){
            for (MenuVo menu:dir.getSubMenus()){
                for (MenuPojo btn:all){
                    if (btn.getPid() == menu.getId()){
                        menu.addSubMenu(btn);
                    }
                }
            }
        }

        return tree;
    }
}
