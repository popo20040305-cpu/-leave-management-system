package edu.hbmd.taotao.qingjia.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.hbmd.taotao.qingjia.pojo.ClassPojo;
import edu.hbmd.taotao.qingjia.pojo.LeaveApplicationShenPiPojo;
import edu.hbmd.taotao.qingjia.pojo.TeacherPojo;
import edu.hbmd.taotao.qingjia.service.BanzhurenShenPiService;
import edu.hbmd.taotao.qingjia.service.ClassService;
import edu.hbmd.taotao.qingjia.shiro.LoginUser;
import edu.hbmd.taotao.qingjia.util.R;
import edu.hbmd.taotao.qingjia.vo.LeaveApplicationVo;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/leave/banzhuren")
public class BanzhurenShenPiController extends BaseController{
    @Autowired
    private BanzhurenShenPiService banzhurenShenPiService;
    @Autowired
    private ClassService classService;

    /**
     * 查询（分页，带状态和班级筛选）
     */
    @GetMapping("/index")
    @RequiresPermissions("leave:banzhuren:access")
    public String query(@RequestParam(defaultValue = "1") Integer curPage,
                        @RequestParam(required = false) Integer status,
                        @RequestParam(required = false) Integer classId,
                        Model model) {
        LoginUser loginUser = getLoginUser();
        TeacherPojo teacher = loginUser.getTeacher();
        // 获取该教师所带班级列表
        List<ClassPojo> allClasses = classService.listByTeacherId(teacher.getId());
        // 如果选择了班级，则只查该班；否则查所有班级（但我们需要传递班级ID列表）
        List<Integer> classIds=new ArrayList<>();
        if (classId != null && classId > 0) {
            classIds.add(classId);
        } else {
            for (ClassPojo clazz:allClasses){
                classIds.add(clazz.getId());
            }
        }

        // 分页查询
        Page<LeaveApplicationVo> page = banzhurenShenPiService.pageByClassIds(curPage, classIds, status);
        model.addAttribute("page", page);
        model.addAttribute("classList", allClasses);
        model.addAttribute("status", status);
        model.addAttribute("statusList", getApplicationStatuses());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedClassId", classId);
        return "leave/banzhuren";
    }

    /**
     * 获取申请详情（含审批记录）
     */
    @GetMapping("/detail/{id}")
    @ResponseBody
    @RequiresPermissions("leave:banzhuren:access")
    public R detail(@PathVariable Integer id) {
        LeaveApplicationVo vo = banzhurenShenPiService.getDetail(id);
        return vo != null ? R.ok().put("data", vo) : R.error("记录不存在");
    }
    /**
     * 班主任审批
     */
    @PostMapping("/approve")
    @ResponseBody
    @RequiresPermissions("leave:banzhuren:access")
    public R approve(@RequestBody LeaveApplicationShenPiPojo shenpi) {
        LoginUser loginUser = getLoginUser();
        Integer teacherId = loginUser.getTeacher().getId();
        try {
            boolean success = banzhurenShenPiService.approve(shenpi.getApplicationId(), teacherId, shenpi.getResult(), shenpi.getOpinion());
            return success ? R.ok("审批成功") : R.error("审批失败");
        } catch (Exception e) {
            return R.error(e.getMessage());
        }
    }

}
