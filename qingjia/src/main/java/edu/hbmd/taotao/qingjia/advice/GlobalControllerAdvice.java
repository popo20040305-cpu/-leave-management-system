package edu.hbmd.taotao.qingjia.advice;

import edu.hbmd.taotao.qingjia.shiro.LoginUser;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.HashSet;
import java.util.Set;

@ControllerAdvice
public class GlobalControllerAdvice {
    @ModelAttribute
    public void addLoginUserInfo(Model model) {
        Subject subject = SecurityUtils.getSubject();

        if (subject.getPrincipal() != null) {
            LoginUser loginUser = (LoginUser) subject.getPrincipal();
            Set<String> permissions = loginUser.getPermissionNames();
            //权限
            model.addAttribute("userPermissions",
                    permissions!=null?permissions:new HashSet<String>());
            //角色
            Set<String> roles = loginUser.getRoleNames();
            model.addAttribute("userRoles",
                    roles!=null?roles:new HashSet<String>());
            //用户信息
            model.addAttribute("loginUser",loginUser);
        }
    }

}
