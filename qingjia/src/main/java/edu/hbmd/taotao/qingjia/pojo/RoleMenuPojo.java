package edu.hbmd.taotao.qingjia.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.apache.ibatis.type.Alias;

@Data
@Alias("RoleMenuPojo")
@TableName("role_menu")
public class RoleMenuPojo {
    @TableId(type = IdType.AUTO)
    private Integer roleId;
    private Integer menuId;
}
