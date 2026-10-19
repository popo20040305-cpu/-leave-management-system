package edu.hbmd.taotao.qingjia.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.hbmd.taotao.qingjia.pojo.LeaveApplicationPojo;
import edu.hbmd.taotao.qingjia.properties.UploadFileProperties;
import edu.hbmd.taotao.qingjia.service.LeaveApplicationService;
import edu.hbmd.taotao.qingjia.shiro.LoginUser;
import edu.hbmd.taotao.qingjia.util.R;
import edu.hbmd.taotao.qingjia.vo.LeaveApplicationVo;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;

@Controller
@RequestMapping("/leave/student")
public class LeaveApplicationController extends BaseController{
    @Autowired
    private LeaveApplicationService leaveApplicationService;

    @Autowired
    private UploadFileProperties uploadFileProperties;

    /**
     * 查询当前学生的请假申请（分页，条件状态）
     */
    @GetMapping("/index")
    @RequiresPermissions("leave:student:access")
    @RequiresRoles("学生")
    public String query(@RequestParam(defaultValue = "1") Integer curPage,
                        @RequestParam(required = false) Integer status,
                        Model model) {
        LoginUser loginUser = getLoginUser();
        Integer studentId = loginUser.getStudent().getId();
        Page<LeaveApplicationVo> page = leaveApplicationService.pageMyApplications(curPage, studentId, status);
        model.addAttribute("page", page);
        model.addAttribute("status", status);
        // 重新填充下拉
        model.addAttribute("leaveTypes", getleaveApplicationTypes());
        model.addAttribute("statusList", getApplicationStatuses());
        return "leave/myLeave";
    }
    /**
     * 保存草稿（待提交）
     */
    @PostMapping("/saveDraft")
    @ResponseBody
    @RequiresPermissions("leave:student:add")
    @RequiresRoles("学生")
    public R saveDraft(@Valid @RequestBody LeaveApplicationPojo application) {
        LoginUser loginUser = getLoginUser();
        application.setStudentId(loginUser.getStudent().getId());
        boolean success = leaveApplicationService.saveDraft(application);
        return success ? R.ok("草稿保存成功") : R.error("保存失败");
    }

    /**
     * 新建时直接提交（待班主任审批）
     */
    @PostMapping("/submit")
    @ResponseBody
    @RequiresPermissions("leave:student:add")
    @RequiresRoles("学生")
    public R submit(@Valid @RequestBody LeaveApplicationPojo application) {
        LoginUser loginUser = getLoginUser();
        application.setStudentId(loginUser.getStudent().getId());
        boolean success = leaveApplicationService.submitApplication(application);
        return success ? R.ok("提交成功") : R.error("提交失败");
    }
    /**
     * 提交草稿（将待提交记录改为待班主任审批）
     */
    @PostMapping("/submitDraft/{id}")
    @ResponseBody
    @RequiresPermissions("leave:student:add")
    @RequiresRoles("学生")
    public R submitDraft(@PathVariable Integer id) {
        boolean success = leaveApplicationService.submitDraft(id);
        return success ? R.ok("提交成功") : R.error("提交失败，仅待提交状态可提交");
    }
    /**
     * 删除草稿
     */
    @DeleteMapping("/delete/{id}")
    @ResponseBody
    @RequiresPermissions("leave:student:delete")
    @RequiresRoles("学生")
    public R delete(@PathVariable Integer id) {
        boolean success = leaveApplicationService.deleteDraft(id);
        return success ? R.ok("删除成功") : R.error("删除失败，仅草稿可删除");
    }

    /**
     * 获取申请详情（含审批记录）
     */
    @GetMapping("/detail/{id}")
    @ResponseBody
    @RequiresPermissions("leave:student:detail")
    @RequiresRoles("学生")
    public R detail(@PathVariable Integer id) {
        LeaveApplicationVo vo = leaveApplicationService.getDetailById(id);
        return vo != null ? R.ok().put("data", vo) : R.error("记录不存在");
    }
    /**
     * 销假（上传照片，归档）
     */
    @PostMapping("/xiaojia")
    @ResponseBody
    @RequiresPermissions("leave:student:xiaojia")
    public R xiaoJia(@RequestParam Integer applicationId,
                     @RequestParam("photo") MultipartFile photo) {
        LoginUser loginUser = getLoginUser();
        // 验证是否属于当前学生（在Service中也可验证）
        LeaveApplicationPojo app = leaveApplicationService.getById(applicationId);
        if (app == null || !app.getStudentId().equals(loginUser.getStudent().getId())) {
            return R.error("非法操作");
        }
        boolean success = leaveApplicationService.xiaoJia(applicationId, photo,loginUser.getStudent().getNum());
        return success ? R.ok("销假成功") : R.error("销假失败");
    }
}
