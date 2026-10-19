package edu.hbmd.taotao.qingjia.vo;

import edu.hbmd.taotao.qingjia.pojo.LeaveApplicationShenPiPojo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.ibatis.type.Alias;

@Data
@EqualsAndHashCode(callSuper = true)
@Alias("LeaveApplicationShenPiVo")
public class LeaveApplicationShenPiVo extends LeaveApplicationShenPiPojo {
    private String teacherName;   // 审批教师姓名
}
