package edu.hbmd.taotao.qingjia.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.apache.ibatis.type.Alias;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
@Alias("StudentPojo")
@TableName("student")
public class StudentPojo {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @NotBlank(message = "姓名不能为空")
    @Size(max = 50, message = "姓名长度不能超过50")
    private String name;
    @NotBlank(message = "学号不能为空")
    @Size(max = 20, message = "学号长度不能超过20")
    private String num;
    private String password;
    private String salt;
    /**
     * 班级不能为空。
     *
     * 这里原来是 @NotBlank —— 那是给 CharSequence 用的约束，挂在 Integer 字段上
     * 会让 Hibernate Validator 在校验时抛 HV000030（No validator could be found
     * for constraint）而不是给出「班级不能为空」的提示。老 StudentController.save
     * 是手工解析 JSON、没有触发 Bean Validation，所以这个坑一直没暴露；
     * 新的 /api/student/save 用 @Valid，必须换成 @NotNull 才正确。
     */
    @NotNull(message = "班级不能为空")
    private Integer classId;
    private Integer sex;
    /**
     * 允许留空。原来写的是 "^1[3-9]\\d{9}$"，@Pattern 对空串也会跑正则，
     * 于是「不填手机号」会被判成格式错误 —— 但 el-input 的 v-model 天然就是空串，
     * 客户端很难保证不发。这里按 CollegePojo.tel 的既有写法放开空串，
     * controller 里再把空串归一成 null（和库里已有的 null 语义一致）。
     * 注意 null 本身本来就是合法的（@Pattern 不校验 null）。
     */
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String mobile;
    @Pattern(regexp = "^$|^[1-9]\\d{4,10}$", message = "QQ号格式不正确")
    private String qq;
    private String avatarUrl;

}
