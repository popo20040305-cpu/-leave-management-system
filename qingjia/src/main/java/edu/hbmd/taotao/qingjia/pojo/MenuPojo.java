package edu.hbmd.taotao.qingjia.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.apache.ibatis.type.Alias;

import javax.validation.constraints.NotBlank;
import java.util.List;

@Data
@Alias("MenuPojo")
@TableName("menu")
public class MenuPojo {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @NotBlank(message = "菜单名不能为空")
    private String name;
    @NotBlank(message = "权限字符串不能为空")
    private String permission;
    private Integer pid;
    private String pids;
    private String url;
    private Integer type;
    private String icon;
    private Integer order_num;
}
