package edu.hbmd.taotao.qingjia.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.apache.ibatis.type.Alias;

import javax.validation.constraints.NotBlank;

@Data
@Alias("ClassPojo")
@TableName("class")
public class ClassPojo {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @NotBlank(message = "班级编号不能为空")
    private String num;
    private Integer grade;
    private Integer belongToCollegeId;
    private Integer banZhuRenId;
}
