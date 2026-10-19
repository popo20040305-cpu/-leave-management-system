package edu.hbmd.taotao.qingjia.service;

import com.baomidou.mybatisplus.extension.service.IService;
import edu.hbmd.taotao.qingjia.pojo.RoleMenuPojo;

import java.util.List;
import java.util.Set;

public interface RoleMenuService extends IService<RoleMenuPojo> {
    Set<Integer> getMenuIdsByRoleIds(List<Integer> roleIds);

    boolean saveRoleMenus(Integer roleId, List<Integer> menuIds);
}
