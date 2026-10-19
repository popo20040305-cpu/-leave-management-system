package edu.hbmd.taotao.qingjia.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.apache.ibatis.type.Alias;

import javax.validation.constraints.NotBlank;

@Data
@Alias("TeacherPojo")
@TableName("teacher")
public class TeacherPojo {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @NotBlank(message = "姓名不能为空")
    private String name;
    @NotBlank(message = "工号不能为空")
    private String num;
    private String password;
    private String salt;
    private Integer collegeId;
    private String mobile;
    private String avatarUrl;
}
