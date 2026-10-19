package edu.hbmd.taotao.qingjia.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import edu.hbmd.taotao.qingjia.pojo.LeaveApplicationPojo;
import edu.hbmd.taotao.qingjia.vo.LeaveApplicationVo;

import java.util.List;

public interface BanzhurenShenPiService extends IService<LeaveApplicationPojo> {
    /* 分页查询班主任所带班级的请假申请
     * @param curPage   当前页码
     * @param classIds  班级ID列表（班主任所带班级）
     * @param status    申请状态（null表示全部）
     * @return 分页数据
     */
    Page<LeaveApplicationVo> pageByClassIds(int curPage, List<Integer> classIds, Integer status);

    /**
     * 同上，可指定每页条数（Vue 页面支持「每页 50 条」）。
     *
     * @param current 页码，从 1 开始，小于 1 按 1 处理
     * @param size    每页条数，小于 1 时退回 PageConstant.pageSize
     */
    Page<LeaveApplicationVo> pageByClassIds(long current, long size, List<Integer> classIds, Integer status);

    /**
     * 获取申请详情（包含审批记录）
     * @param applicationId 申请ID
     * @return Vo对象
     */
    LeaveApplicationVo getDetail(Integer applicationId);
    /**
     * 班主任审批操作
     * @param applicationId 请假申请ID
     * @param teacherId     审批教师ID（班主任）
     * @param result        1-通过，2-不通过
     * @param opinion       审批意见
     * @return true-成功
     */
    boolean approve(Integer applicationId, Integer teacherId, Integer result, String opinion);

}
