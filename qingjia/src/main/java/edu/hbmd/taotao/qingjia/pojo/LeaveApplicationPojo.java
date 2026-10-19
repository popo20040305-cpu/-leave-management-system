package edu.hbmd.taotao.qingjia.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.apache.ibatis.type.Alias;

import javax.validation.constraints.*;
import java.util.Date;

@Data
@Alias("LeaveApplicationPojo")
@TableName("leave_application")
public class LeaveApplicationPojo {
    @TableId(type = IdType.AUTO)
    private Integer id;  // 不校验，由 Service 处理

    private Integer studentId;//不校验，系统根据登录用户自动获取

    @NotNull(message = "请假类型不能为空")
    @Min(value = 1, message = "请假类型必须为有效值")
    private Integer type;

    @NotNull(message = "请假途径不能为空")
    @Size(max = 50, message = "请假途径长度不能超过50")
    private String path;

    @NotBlank(message = "请假原因不能为空")
    @Size(max = 200, message = "请假原因长度不能超过200")
    private String reason;

    @NotNull(message = "开始时间不能为空")
    private Date begintime;

    @NotNull(message = "结束时间不能为空")
    private Date endtime;

    @lombok.Getter
    @Min(value = 1, message = "状态值非法")
    @Max(value = 7, message = "状态值非法")
    private Integer status;

    @Size(max = 255, message = "照片URL长度不能超过255")
    private String xiaoJiaPhotoUrl;

    private Date xiaoJiaTime;

    @AssertTrue(message = "结束时间必须晚于开始时间")
    public boolean isEndTimeAfterBeginTime() {
        if (begintime == null || endtime == null) return true;
        return endtime.after(begintime);
    }

}
