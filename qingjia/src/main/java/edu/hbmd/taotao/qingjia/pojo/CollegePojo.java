package edu.hbmd.taotao.qingjia.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.apache.ibatis.type.Alias;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
@Alias("CollegePojo")
@TableName("college")
public class CollegePojo {
    @TableId(type = IdType.AUTO)
    private Integer id;         // id

    @NotBlank(message = "学院名称不能为空")
    private String name;        // 学院名

    @NotBlank(message = "学院编号不能为空")
    private String num;         // 学院编号

    /**
     * 办公电话。允许留空（^$），否则必须是 3-20 位的电话字符：
     * 数字、空格、圆括号、加号、连字符。
     *
     * 原来写的是 "^$|\\d{3,15}"（只允许纯数字），但库里现存的种子数据本身就带分隔符 ——
     * id=3 信息工程学院 tel='0718 - 8437432'、id=6 医学院 tel='0718-87659867'。
     * 结果是这两条记录点开「修改」弹窗、什么都不改直接保存，也会被 @Valid 拦下报
     * “办公电话格式不正确”，修改功能对 1/3 的现有数据是不可用的。
     * 这里放宽字符集而不是去改数据：只放宽不收紧，以前能通过的（纯数字）现在照样通过，
     * 老的 Thymeleaf /college/save 用的也是这个 pojo，行为只会变宽松，不会变严格。
     *
     * 注意 @Pattern 用的是 Matcher.matches()，整串都必须匹配，所以 {3,20} 是全长限制，
     * 不存在“前 3-15 位是数字就算通过”的漏洞。null 值 @Pattern 直接放过，
     * 后端 normalize() 会把空串转成 null 入库。
     */
    @Pattern(regexp = "^$|^[0-9 ()\\-+]{3,20}$", message = "办公电话格式不正确（3-20位数字，可含空格、-、()、+）")
    private String tel;         // 办公电话
}
