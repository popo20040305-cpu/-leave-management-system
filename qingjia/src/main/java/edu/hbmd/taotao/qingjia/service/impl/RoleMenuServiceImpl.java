package edu.hbmd.taotao.qingjia.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.hbmd.taotao.qingjia.mapper.MenuMapper;
import edu.hbmd.taotao.qingjia.mapper.RoleMapper;
import edu.hbmd.taotao.qingjia.mapper.RoleMenuMapper;
import edu.hbmd.taotao.qingjia.pojo.MenuPojo;
import edu.hbmd.taotao.qingjia.pojo.RoleMenuPojo;
import edu.hbmd.taotao.qingjia.service.RoleMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class RoleMenuServiceImpl extends ServiceImpl<RoleMenuMapper, RoleMenuPojo>
implements RoleMenuService {

    /**
     * 直接注入 MenuMapper 而不是 MenuService：MenuServiceImpl 里已经注入了
     * RoleMenuService，这边再注入 MenuService 会形成循环依赖。
     * （Spring Boot 2.2 默认允许循环引用，但没必要冒这个险。）
     */
    @Autowired
    private MenuMapper menuMapper;

    @Override
    public Set<Integer> getMenuIdsByRoleIds(List<Integer> roleIds) {
        QueryWrapper<RoleMenuPojo> wrapper = new QueryWrapper<>();
        wrapper.in("roleId",roleIds);
        List<RoleMenuPojo> roleMenuPojos = baseMapper.selectList(wrapper);
        Set<Integer> menuIds = new HashSet<>();
        for (RoleMenuPojo roleMenuPojo:roleMenuPojos){
            menuIds.add(roleMenuPojo.getMenuId());
        }
        return menuIds;
    }

    /**
     * 保存角色的权限（先清空旧的，再写入新的）。
     *
     * 会把勾选节点的祖先一并写进去。原因：前端 el-tree 是勾选联动的，用户实际勾的是
     * 叶子（按钮 / 菜单），如果只把这些叶子的 id 存进 role_menu，
     * {@code MenuServiceImpl.getMenuNamesByUserIdAndUserType} 组装左侧菜单时
     * 顶层目录（pid=0）就不在用户的菜单集合里，整棵树都渲染不出来 —— 权限明明勾了，
     * 登录后却一个菜单都看不到。所以这里按每个节点的 pids 链补齐所有祖先。
     */
    @Override
    @Transactional
    public boolean saveRoleMenus(Integer roleId, List<Integer> menuIds) {
        //删除角色所有的旧的权限
        LambdaQueryWrapper<RoleMenuPojo> wrapper = new LambdaQueryWrapper();
        wrapper.eq(RoleMenuPojo::getRoleId,roleId);
        this.remove(wrapper);

        //添加新的角色权限
        if (menuIds!=null&&menuIds.size()>0){
            // LinkedHashSet：去重的同时保持前端传来的顺序
            Set<Integer> allIds = new LinkedHashSet<>();
            for (Integer menuId : menuIds) {
                allIds.add(menuId);
                collectAncestors(menuId, allIds);
            }
            List<RoleMenuPojo> datas = new ArrayList<>();
            for (Integer menuId : allIds){
                RoleMenuPojo roleMenu = new RoleMenuPojo();
                roleMenu.setRoleId(roleId);
                roleMenu.setMenuId(menuId);
                datas.add(roleMenu);
            }
            this.saveBatch(datas);
        }
        return true;
    }

    /**
     * 把 menuId 的所有祖先 id 加进 result。
     * 优先走 pids 字段（形如 "0,1,7,"，就是给这种场景准备的），
     * pids 为空的老数据退回 pid 逐级向上找。
     */
    private void collectAncestors(Integer menuId, Set<Integer> result) {
        MenuPojo menu = menuMapper.selectById(menuId);
        if (menu == null) {
            return;
        }
        String pids = menu.getPids();
        if (pids != null && !pids.trim().isEmpty()) {
            for (String part : pids.split(",")) {
                if (!part.trim().isEmpty()) {
                    try {
                        Integer pid = Integer.valueOf(part.trim());
                        if (!"0".equals(part.trim())) {
                            result.add(pid);
                        }
                    } catch (NumberFormatException ignored) {
                        // pids 里混进非数字就跳过，不能因为一条脏数据把整个保存流程打断
                    }
                }
            }
            return;
        }
        // 兜底：老数据没有 pids，用 pid 逐级向上
        Integer pid = menu.getPid();
        int guard = 0;
        while (pid != null && pid != 0 && guard++ < 10) {
            if (!result.add(pid)) {
                break; // 已经加过了，说明到顶了
            }
            MenuPojo parent = menuMapper.selectById(pid);
            if (parent == null) {
                break;
            }
            pid = parent.getPid();
        }
    }
}
