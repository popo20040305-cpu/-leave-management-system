package edu.hbmd.taotao.qingjia.pojo;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 修复点：expire_time / update_time 原来被声明成 lombok.Data（一个注解类型），
 * 导致 UserTokenServiceImpl 里出现 (Data) date 这种强转，运行时必然 ClassCastException，
 * 且 MyBatis-Plus 也无法把 datetime 列映射进来。这里改回 java.util.Date。
 */
@Data
@TableName("user_token")
public class UserTokenPojo {
    private Integer userId;
    private Integer userType;
    private String token;
    private Date expire_time;
    private Date update_time;
}
