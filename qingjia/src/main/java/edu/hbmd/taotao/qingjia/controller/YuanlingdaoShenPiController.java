package edu.hbmd.taotao.qingjia.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.hbmd.taotao.qingjia.pojo.ClassPojo;
import edu.hbmd.taotao.qingjia.pojo.GradePojo;
import edu.hbmd.taotao.qingjia.pojo.LeaveApplicationShenPiPojo;
import edu.hbmd.taotao.qingjia.service.ClassService;
import edu.hbmd.taotao.qingjia.service.YuanlingdaoShenPiService;
import edu.hbmd.taotao.qingjia.shiro.LoginUser;
import edu.hbmd.taotao.qingjia.util.R;
import edu.hbmd.taotao.qingjia.vo.LeaveApplicationVo;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/leave/yuanlingdao")
public class YuanlingdaoShenPiController extends BaseController {
    @Autowired
    private YuanlingdaoShenPiService yuanlingdaoShenPiService;
    @Autowired
    private ClassService classService;

    /**
     * 查询（分页，带状态、年级、班级筛选）
     */
    @GetMapping("/index")
    @RequiresPermissions("leave:yuanlingdao:access")
    public String query(@RequestParam(defaultValue = "1") Integer curPage,
                        @RequestParam(required = false) Integer status,
                        @RequestParam(required = false) Integer grade,
                        @RequestParam(required = false) Integer classId,
                        Model model) {
        LoginUser loginUser = getLoginUser();
        Integer collegeId = loginUser.getTeacher().getCollegeId();

        // 分页查询
        Page<LeaveApplicationVo> page = yuanlingdaoShenPiService.pageByCollege(curPage, collegeId, grade, classId, status);
        model.addAttribute("page", page);

        // 重新填充下拉数据
        List<GradePojo> grades = getGrades();
        model.addAttribute("grades", grades);
        List<ClassPojo> classes = classService.listByCollegeAndGrade(collegeId, grade);
        model.addAttribute("classList", classes);
        model.addAttribute("statusList", getApplicationStatuses());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedGrade", grade);
        model.addAttribute("selectedClassId", classId);
        model.addAttribute("loginUser", loginUser);

        return "leave/yuanlingdao";
    }

    /**
     * 根据学院和年级加载班级（AJAX联动）
     */
    @GetMapping("/getClasses")
    @ResponseBody
    @RequiresPermissions("leave:yuanlingdao:access")
    public R getClasses(@RequestParam(required = false) Integer grade) {
        LoginUser loginUser = getLoginUser();
        Integer collegeId = loginUser.getTeacher().getCollegeId();
        List<ClassPojo> classes = classService.listByCollegeAndGrade(collegeId, grade);
        return R.ok().put("data", classes);
    }
    /**
     * 获取申请详情（含审批记录）
     */
    @GetMapping("/detail/{id}")
    @ResponseBody
    @RequiresPermissions("leave:yuanlingdao:access")
    public R detail(@PathVariable Integer id) {
        LeaveApplicationVo vo = yuanlingdaoShenPiService.getDetail(id);
        return vo != null ? R.ok().put("data", vo) : R.error("记录不存在");
    }
    /**
     * 院领导审批
     */
    @PostMapping("/approve")
    @ResponseBody
    @RequiresPermissions("leave:yuanlingdao:access")
    @RequiresRoles("学院领导")
    public R approve(@RequestBody LeaveApplicationShenPiPojo shenpi) {
        LoginUser loginUser = getLoginUser();
        Integer teacherId = loginUser.getTeacher().getId();
        try {
            boolean success = yuanlingdaoShenPiService.approve(
                    shenpi.getApplicationId(),
                    teacherId,
                    shenpi.getResult(),
                    shenpi.getOpinion()
            );
            return success ? R.ok("审批成功") : R.error("审批失败");
        } catch (Exception e) {
            return R.error(e.getMessage());
        }
    }

}
