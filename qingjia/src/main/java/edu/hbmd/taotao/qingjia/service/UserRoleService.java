package edu.hbmd.taotao.qingjia.service;

import com.baomidou.mybatisplus.extension.service.IService;
import edu.hbmd.taotao.qingjia.pojo.UserRolePojo;

import java.util.List;

public interface UserRoleService extends IService<UserRolePojo> {
    List<Integer> getRoleIdsByUserIdAndUserType(Integer userId, Integer userType);

    boolean saveRoles(Integer teacherId, Integer[] roleIds);
}
