package edu.hbmd.taotao.qingjia.shiro;

import edu.hbmd.taotao.qingjia.constant.UserTypeConstant;
import edu.hbmd.taotao.qingjia.pojo.RolePojo;
import edu.hbmd.taotao.qingjia.pojo.StudentPojo;
import edu.hbmd.taotao.qingjia.pojo.TeacherPojo;
import edu.hbmd.taotao.qingjia.service.*;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.util.ByteSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class QingjiaRealm extends AuthorizingRealm {
    @Autowired
    StudentService studentService;
    @Autowired
    TeacherService teacherService;

    @Autowired
    RoleService roleService;
    @Autowired
    MenuService menuService;


    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principalCollection) {
        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
        LoginUser loginUser = (LoginUser) principalCollection.getPrimaryPrincipal();

        info.setRoles(loginUser.getRoleNames());

        info.setStringPermissions(loginUser.getPermissionNames());
        return info;
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken) throws AuthenticationException {
        QingjiaUsernamePasswordToken  qingjaToken = (QingjiaUsernamePasswordToken)authenticationToken;
        AuthenticationInfo authenticationInfo = null;
        LoginUser loginUser = new LoginUser();
        if (qingjaToken.getType() == UserTypeConstant.USER_TYPE_STUDENT){//学生
            StudentPojo studentPojo = studentService.getByNum(qingjaToken.getUsername());
            if (studentPojo == null){
                throw new RuntimeException("用户不存在");
            }
            loginUser.setType(LoginUser.TYPE_STUDENT);
            loginUser.setStudent(studentPojo);
            //用户id与类型查询用户的角色名
            Integer userId = loginUser.getStudent().getId();
            loginUser.setRoleType(roleService.getRoleTypeByUserIdAndUserType(userId,loginUser.getType()));
            Set<String> roleNames = roleService.getRoleNamesByUserIdAndUserType(userId,loginUser.getType());
            loginUser.setRoleNames(roleNames);
            //用户id与类型查询用户的权限名
            Set<String> permissionNames = null;
            permissionNames = menuService.getMenuNamesByUserIdAndUserType(userId,loginUser.getType());
           loginUser.setPermissionNames(permissionNames);
            authenticationInfo = new SimpleAuthenticationInfo(loginUser,studentPojo.getPassword(), ByteSource.Util.bytes(studentPojo.getSalt()),"lwgl");
        }else{//教师
            TeacherPojo teacherPojo = teacherService.getByNum(qingjaToken.getUsername());
            if (teacherPojo == null){
                throw new RuntimeException("用户不存在");
            }
            loginUser.setType(LoginUser.TYPE_TEACHER);
            loginUser.setTeacher(teacherPojo);
            Integer userId = loginUser.getTeacher().getId();
            loginUser.setRoleType(roleService.getRoleTypeByUserIdAndUserType(userId,loginUser.getType()));
            //用户id与类型查询用户的角色名
            Set<String> roleNames = roleService.getRoleNamesByUserIdAndUserType(userId,loginUser.getType());
            loginUser.setRoleNames(roleNames);
            //用户id与类型查询用户的权限名
            Set<String> permissionNames = menuService.getMenuNamesByUserIdAndUserType(userId,loginUser.getType());
            loginUser.setPermissionNames(permissionNames);
            authenticationInfo = new SimpleAuthenticationInfo(loginUser,teacherPojo.getPassword(),ByteSource.Util.bytes(teacherPojo.getSalt()),"lwgl");
        }
        return authenticationInfo;
    }
}
