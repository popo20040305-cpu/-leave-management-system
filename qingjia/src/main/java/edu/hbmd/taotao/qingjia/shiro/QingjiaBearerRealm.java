package edu.hbmd.taotao.qingjia.shiro;

import edu.hbmd.taotao.qingjia.pojo.StudentPojo;
import edu.hbmd.taotao.qingjia.pojo.TeacherPojo;
import edu.hbmd.taotao.qingjia.pojo.UserTokenPojo;
import edu.hbmd.taotao.qingjia.service.*;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Set;

@Component
public class QingjiaBearerRealm extends AuthorizingRealm {

    @Autowired
    private UserTokenService userTokenService;
    @Autowired
    private StudentService studentService;
    @Autowired
    private TeacherService teacherService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private MenuService menuService;

    @Override
    public boolean supports(AuthenticationToken token) {
        return token instanceof QingjiaBearerToken;
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        LoginUser loginUser = (LoginUser) principals.getPrimaryPrincipal();
        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
        info.setRoles(loginUser.getRoleNames());
        info.setStringPermissions(loginUser.getPermissionNames());
        return info;
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        String tokenStr = (String) token.getPrincipal();
        UserTokenPojo userToken = userTokenService.getByToken(tokenStr);
        if (userToken == null) {
            throw new AuthenticationException("Token不存在或已失效");
        }

        Date expireTime = (Date) userToken.getExpire_time();
        if (expireTime == null || expireTime.before(new Date())) {
            throw new AuthenticationException("Token已过期");
        }

        Integer userId = userToken.getUserId();
        Integer userType = userToken.getUserType();
        LoginUser loginUser = new LoginUser();

        if (userType == LoginUser.TYPE_STUDENT) {
            StudentPojo student = studentService.getById(userId);
            if (student == null) {
                throw new AuthenticationException("学生信息不存在");
            }
            loginUser.setType(LoginUser.TYPE_STUDENT);
            loginUser.setStudent(student);
        } else if (userType == LoginUser.TYPE_TEACHER) {
            TeacherPojo teacher = teacherService.getById(userId);
            if (teacher == null) {
                throw new AuthenticationException("教师信息不存在");
            }
            loginUser.setType(LoginUser.TYPE_TEACHER);
            loginUser.setTeacher(teacher);
        } else {
            throw new AuthenticationException("未知用户类型");
        }

        // 查询角色和权限
        Set<String> roleNames = roleService.getRoleNamesByUserIdAndUserType(userId, userType);
        loginUser.setRoleNames(roleNames);
        // 若 RoleService 接口方法名已修正为 getRoleTypeByUserIdAndUserType，则直接调用
        loginUser.setRoleType(roleService.getRoleTypeByUserIdAndUserType(userId, userType));
        Set<String> permissionNames = menuService.getMenuNamesByUserIdAndUserType(userId, userType);
        loginUser.setPermissionNames(permissionNames);

        return new SimpleAuthenticationInfo(loginUser, tokenStr, getName());
    }
}