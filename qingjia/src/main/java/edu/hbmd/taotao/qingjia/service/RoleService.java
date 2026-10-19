package edu.hbmd.taotao.qingjia.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import edu.hbmd.taotao.qingjia.pojo.RolePojo;

import java.util.List;
import java.util.Set;

public interface RoleService extends IService<RolePojo> {
    List<RolePojo> getRolesByIds(List<Integer> ids);
    Set<String> getRoleNamesByUserIdAndUserType(Integer userId, Integer userType);
    int getRoleTypeByUserIdAndUserType(Integer userId, int type);

    int getRoleTypeByUserIdAndUserTpye(Integer userId, int userType);

    // 分页查询方法
    IPage<RolePojo> pageRoles(Integer curPage, Integer limit, String name);
    //保存数据（添加、修改）
    boolean saveOrUpdateRole(RolePojo role);
    RolePojo getRoleById(Integer id);
    boolean deleteRoleAndRelations(Integer roleId);
}
