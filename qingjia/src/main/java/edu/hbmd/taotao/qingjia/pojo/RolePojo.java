package edu.hbmd.taotao.qingjia.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.apache.ibatis.type.Alias;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@Alias("RolePojo")
@TableName("role")
public class RolePojo {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @NotBlank(message = "角色名称不能为空")
    private String name;
    private String remark;
    @NotNull(message = "角色类型不能为空")
    private Integer roleType;//数据访问范围
}
