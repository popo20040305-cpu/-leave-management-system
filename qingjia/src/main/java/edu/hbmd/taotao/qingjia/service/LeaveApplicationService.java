package edu.hbmd.taotao.qingjia.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import edu.hbmd.taotao.qingjia.pojo.LeaveApplicationPojo;
import edu.hbmd.taotao.qingjia.vo.LeaveApplicationVo;
import org.springframework.web.multipart.MultipartFile;

public interface LeaveApplicationService extends IService<LeaveApplicationPojo> {
    // 分页查询当前学生的申请
    Page<LeaveApplicationVo> pageMyApplications(int curPage, Integer studentId, Integer status);

    /**
     * 分页查询当前学生的申请，可指定每页条数。
     *
     * 理由同 TeacherService / ClassService 里的同签名重载：两参版本写死了
     * PageConstant.pageSize（10 条），Vue 页面选「每页 50 条」时后端收不到 size，
     * 会出现 total 正确但 records 永远只有 10 行的情况。
     *
     * @param current 页码，从 1 开始，小于 1 按 1 处理
     * @param size    每页条数，小于 1 时退回 PageConstant.pageSize
     */
    Page<LeaveApplicationVo> pageMyApplications(long current, long size, Integer studentId, Integer status);

    // 保存草稿（待提交）
    boolean saveDraft(LeaveApplicationPojo application);
    // 提交申请（状态保存为待班主任审批）
    boolean submitApplication(LeaveApplicationPojo application);
    // 将待提交记录改为待班主任审批（提交操作）
    boolean submitDraft(Integer applicationId);
    // 删除草稿
    boolean deleteDraft(Integer id);
    // 根据ID获取详情（含审批记录）
    LeaveApplicationVo getDetailById(Integer id);
    // 销假（上传照片，改为归档）
    boolean xiaoJia(Integer applicationId, MultipartFile photo,String stuNum);

}
