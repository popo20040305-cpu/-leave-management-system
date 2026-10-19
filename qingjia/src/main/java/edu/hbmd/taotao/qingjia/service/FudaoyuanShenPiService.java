package edu.hbmd.taotao.qingjia.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import edu.hbmd.taotao.qingjia.pojo.LeaveApplicationPojo;
import edu.hbmd.taotao.qingjia.vo.LeaveApplicationVo;

public interface FudaoyuanShenPiService extends IService<LeaveApplicationPojo> {
    /**
     * 辅导员分页查询所管理学院的请假申请（可按照年级、班级、状态筛选）
     * @param curPage   当前页码
     * @param collegeId 学院ID（固定为登录教师所在学院）
     * @param grade     年级（可选）
     * @param classId   班级ID（可选）
     * @param status    状态ID（可选，默认待辅导员审批）
     * @return 分页结果
     */
    Page<LeaveApplicationVo> pageByCollege(int curPage, Integer collegeId, Integer grade, Integer classId, Integer status);

    /**
     * 同上，可指定每页条数（Vue 页面支持「每页 50 条」）。
     *
     * @param current 页码，从 1 开始，小于 1 按 1 处理
     * @param size    每页条数，小于 1 时退回 PageConstant.pageSize
     */
    Page<LeaveApplicationVo> pageByCollege(long current, long size, Integer collegeId, Integer grade,
                                           Integer classId, Integer status);

    /**
     * 获取申请详情（含审批记录）
     * @param applicationId 申请ID
     * @return Vo
     */
    LeaveApplicationVo getDetail(Integer applicationId);
    /**
     * 辅导员审批操作
     * @param applicationId 请假申请ID
     * @param teacherId     辅导员教师ID
     * @param result        1通过，2不通过
     * @param opinion       审批意见
     * @return true成功
     */
    boolean approve(Integer applicationId, Integer teacherId, Integer result, String opinion);

}
