package edu.hbmd.taotao.qingjia.shiro;

import lombok.Data;
import org.apache.shiro.authc.UsernamePasswordToken;
@Data
public class QingjiaUsernamePasswordToken extends UsernamePasswordToken {
    private int type; //登录用户类型 1：学生    2 ：教师
}
