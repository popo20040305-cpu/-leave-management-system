package edu.hbmd.taotao.qingjia.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.hbmd.taotao.qingjia.constant.PageConstant;
import edu.hbmd.taotao.qingjia.constant.RoleTypeConstant;
import edu.hbmd.taotao.qingjia.constant.UserTypeConstant;
import edu.hbmd.taotao.qingjia.exception.ResultEnum;
import edu.hbmd.taotao.qingjia.exception.ResultException;
import edu.hbmd.taotao.qingjia.mapper.RoleMapper;
import edu.hbmd.taotao.qingjia.pojo.RoleMenuPojo;
import edu.hbmd.taotao.qingjia.pojo.RolePojo;
import edu.hbmd.taotao.qingjia.pojo.UserRolePojo;
import edu.hbmd.taotao.qingjia.service.RoleMenuService;
import edu.hbmd.taotao.qingjia.service.RoleService;
import edu.hbmd.taotao.qingjia.service.UserRoleService;
import edu.hbmd.taotao.qingjia.shiro.ShiroProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, RolePojo>
implements RoleService {
    @Autowired
    UserRoleService userRoleService;
    @Autowired
    RoleMenuService roleMenuService;
    @Autowired
    ShiroProperties shiroProperties;
    @Override
    public List<RolePojo> getRolesByIds(List<Integer> ids) {
        return baseMapper.selectBatchIds(ids);
    }

    @Override
    public Set<String> getRoleNamesByUserIdAndUserType(Integer userId, Integer userType) {
        List<Integer> roleIds = userRoleService.getRoleIdsByUserIdAndUserType(userId,userType);
        if (roleIds==null){
            roleIds = new ArrayList<>();
        }
        LambdaQueryWrapper<RolePojo> wrapper = new LambdaQueryWrapper<>();
        if (userType==UserTypeConstant.USER_TYPE_STUDENT){
            wrapper.eq(RolePojo::getName,shiroProperties.getStudentDefaultRoleName());
        }else{
            wrapper.eq(RolePojo::getName,shiroProperties.getTeacherDefaultRoleName());
        }
        RolePojo rolePojo = this.getOne(wrapper);
        if (rolePojo != null){
            roleIds.add(rolePojo.getId());
        }

        List<RolePojo> roles = getRolesByIds(roleIds);
        Set<String>  roleNames = new HashSet<>();
        for (RolePojo role:roles){
            roleNames.add(role.getName());
        }

        return roleNames;
    }

    @Override
    public int getRoleTypeByUserIdAndUserType(Integer userId, int userType) {
        // 原来这里是 return 0 的桩，真逻辑在下面 getRoleTypeByUserIdAndUserTpye
        // （方法名把 Type 拼成了 Tpye）里，而全项目没有任何地方调用拼错的那个。
        // 0 不属于 RoleTypeConstant 的任何取值（1 个人 / 2 班级 / 3 学院 / 4 全部），
        // 于是 BaseController 里所有 roleType == ROLE_TYPE_ALL / == ROLE_TYPE_COLLEGE
        // 的数据范围分支恒假、全是死代码。这里委托给真实现，不动下面那个方法，
        // 改动面最小；要彻底修的话应当把两个方法名合并成一个，那会动到 RoleService 接口。
        return getRoleTypeByUserIdAndUserTpye(userId, userType);
    }

    @Override
    public int getRoleTypeByUserIdAndUserTpye(Integer userId, int userType) {
        List<Integer> roleIds = userRoleService.getRoleIdsByUserIdAndUserType(userId,userType);
        if (roleIds==null){
            roleIds = new ArrayList<>();
        }
        LambdaQueryWrapper<RolePojo> wrapper = new LambdaQueryWrapper<>();
        if (userType==UserTypeConstant.USER_TYPE_STUDENT){
            wrapper.eq(RolePojo::getName,shiroProperties.getStudentDefaultRoleName());
        }else{
            wrapper.eq(RolePojo::getName,shiroProperties.getTeacherDefaultRoleName());
        }
        RolePojo rolePojo = this.getOne(wrapper);
        if (rolePojo!=null){
            roleIds.add(rolePojo.getId());
        }

        List<RolePojo> roles = getRolesByIds(roleIds);
        int roleType = RoleTypeConstant.ROLE_TYPE_PERSON;
        for (RolePojo role:roles){
            if (roleType<role.getRoleType()){
                roleType = role.getRoleType();
            }
        }
        return roleType;
    }

    @Override
    public IPage<RolePojo> pageRoles(Integer curPage, Integer limit, String name) {
        LambdaQueryWrapper<RolePojo> queryWrapper = new LambdaQueryWrapper();
        if (StringUtils.hasText(name)) {
            queryWrapper.like(RolePojo::getName, name);
        }
        queryWrapper.orderByAsc(RolePojo::getId);
        // 如果 limit 为空，使用全局默认页面大小
        if (limit == null || limit <= 0) {
            limit = PageConstant.pageSize;
        }
        Page<RolePojo> page = new Page<>(curPage, limit);

        return this.page(page,queryWrapper);
    }

    @Override
    public boolean saveOrUpdateRole(RolePojo role) {
        if (role.getId()==null){
            long count = this.count(new LambdaQueryWrapper<RolePojo>()
            .eq(RolePojo::getName,role.getName()));
            if (count>0){
                throw new ResultException(ResultEnum.ROLE_ADD_SAME_ROLNAME);
            }
            this.save(role);
        }else {
            this.updateById(role);
        }
        return true;
    }

    @Override
    public RolePojo getRoleById(Integer id) {
        return this.getById(id);
    }

    @Override
    @Transactional
    public boolean deleteRoleAndRelations(Integer roleId) {
        //1、删除角色自身
        boolean remove = this.remove(new LambdaQueryWrapper<RolePojo>().eq(RolePojo::getId,roleId));
        //2、删除角色权限表中与角色id关联的信息
        if (remove){
            roleMenuService.remove(new LambdaQueryWrapper<RoleMenuPojo>()
            .eq(RoleMenuPojo::getRoleId,roleId));

        //3、删除用户角色表中与角色id关联的信息
            userRoleService.remove(new LambdaQueryWrapper<UserRolePojo>()
                    .eq(UserRolePojo::getRoleid,roleId));
        }
        return true;
    }
}
