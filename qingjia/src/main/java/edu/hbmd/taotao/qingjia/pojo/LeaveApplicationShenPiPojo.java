package edu.hbmd.taotao.qingjia.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.apache.ibatis.type.Alias;

import javax.validation.constraints.NotNull;
import java.util.Date;

@Data
@Alias("LeaveApplicationShenPiPojo")
@TableName("leave_application_shen_pi")
public class LeaveApplicationShenPiPojo {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @NotNull(message = "请假申请id不能为空")
    private Integer applicationId;
    @NotNull(message = "审批教师id不能为空")
    private Integer teacherId;
    @NotNull(message = "审批结果类型不能为空")
    private Integer result;      // 1-通过，2-不通过
    @NotNull(message = "审批意见不能为空")
    private String opinion;
    private Date createTime;
    private Integer roleType;//原数据库表中没有这个字段，请在数据库中添加这个字段

}
