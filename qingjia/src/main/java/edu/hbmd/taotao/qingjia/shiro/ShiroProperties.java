package edu.hbmd.taotao.qingjia.shiro;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component("shiroProperties")
@ConfigurationProperties(prefix = "project.shiro")
public class ShiroProperties {
    private String []  excludePathPatterns;//不拦截url
    private String loginUrl;//登录页面url
    private String loginSuccessUrl;
    private String unauthorizedUrl;//未授权页面url
    private String banzhurenDefaultRoleName;//班主任默认角色名
    private String studentDefaultRoleName; //学生默认角色名
    private String teacherDefaultRoleName; //教师默认角色名
    private Integer tokenExpireTime; //token过期时间
}
