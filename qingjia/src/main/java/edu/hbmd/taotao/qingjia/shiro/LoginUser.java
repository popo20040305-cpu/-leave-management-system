package edu.hbmd.taotao.qingjia.shiro;

import edu.hbmd.taotao.qingjia.constant.RoleTypeConstant;
import edu.hbmd.taotao.qingjia.pojo.StudentPojo;
import edu.hbmd.taotao.qingjia.pojo.TeacherPojo;
import lombok.Data;

import java.io.Serializable;
import java.util.Set;

@Data
public class LoginUser implements Serializable {

    public  static final int  TYPE_STUDENT = 1;//类型为学生
    public  static final int  TYPE_TEACHER = 2;//类型为教师
    private int type;//登录用户类型  1：学生2： 教师
    private StudentPojo student;//登录学生基本信息
    private TeacherPojo teacher;//登录教师基本信息
    private int         roleType = RoleTypeConstant.ROLE_TYPE_PERSON;//角色类型

    Set<String> roleNames; //角色字符串数组;
    private Set<String> permissionNames;//权限字符串数组

}
