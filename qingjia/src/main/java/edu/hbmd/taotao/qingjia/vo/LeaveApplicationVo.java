package edu.hbmd.taotao.qingjia.vo;

import edu.hbmd.taotao.qingjia.pojo.LeaveApplicationPojo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.ibatis.type.Alias;

import javax.lang.model.element.NestingKind;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Alias("LeaveApplicationVo")
public class LeaveApplicationVo extends LeaveApplicationPojo {
    // 请假类型名称
    private String typeName;
    // 状态名称
    private String statusName;
    //学生班级
    private String className;
    //学生姓名
    private String studentName;
    // 审批记录列表（包含教师姓名）
    private List<LeaveApplicationShenPiVo> shenPiList;
}
