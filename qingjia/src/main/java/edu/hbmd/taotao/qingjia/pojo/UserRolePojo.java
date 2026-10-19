package edu.hbmd.taotao.qingjia.pojo;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.apache.ibatis.type.Alias;

@Data
@Alias("UserRolePojo")
@TableName("user_role")
public class UserRolePojo {
    private Integer userid;
    private Integer userType;
    private Integer roleid;
}
