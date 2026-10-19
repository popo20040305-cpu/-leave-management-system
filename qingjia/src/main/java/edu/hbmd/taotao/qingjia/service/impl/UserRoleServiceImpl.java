package edu.hbmd.taotao.qingjia.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.hbmd.taotao.qingjia.constant.UserTypeConstant;
import edu.hbmd.taotao.qingjia.mapper.UserRoleMapper;
import edu.hbmd.taotao.qingjia.pojo.UserRolePojo;
import edu.hbmd.taotao.qingjia.service.UserRoleService;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserRoleServiceImpl extends ServiceImpl<UserRoleMapper, UserRolePojo>
implements UserRoleService {
    @Override
    public List<Integer> getRoleIdsByUserIdAndUserType(Integer userId, Integer userType) {
        List<Integer> roleIds = new ArrayList<>();
        QueryWrapper<UserRolePojo> wrapper = new QueryWrapper<>();
        wrapper.eq("userid",userId)
                .eq("userType",userType);
        List<UserRolePojo> userRolePojos = baseMapper.selectList(wrapper);
        for (UserRolePojo userRolePojo:userRolePojos) {
            roleIds.add(userRolePojo.getRoleid());
        }
        return roleIds;
    }

    @Override
    @Transactional
    public boolean saveRoles(Integer teacherId, Integer[] roleIds) {
        //删除旧数据
        LambdaQueryWrapper<UserRolePojo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRolePojo::getUserid,teacherId)
                .eq(UserRolePojo::getUserType, UserTypeConstant.USER_TYPE_TEACHER);
        this.remove(wrapper);
        List<UserRolePojo> userRolePojos = new ArrayList<>();

            if (roleIds==null||roleIds.length<=0){
                return true;
            }
            //保存新数据
            for (Integer roleId:roleIds){
                UserRolePojo userRolePojo = new UserRolePojo();
                userRolePojo.setUserid(teacherId);
                userRolePojo.setUserType(UserTypeConstant.USER_TYPE_TEACHER);
                userRolePojo.setRoleid(roleId);
                userRolePojos.add(userRolePojo);
            }
            return this.saveBatch(userRolePojos);
    }
}
