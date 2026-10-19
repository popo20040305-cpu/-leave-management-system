package edu.hbmd.taotao.qingjia.api;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.hbmd.taotao.qingjia.constant.PageConstant;
import edu.hbmd.taotao.qingjia.controller.BaseController;
import edu.hbmd.taotao.qingjia.exception.ResultEnum;
import edu.hbmd.taotao.qingjia.exception.ResultException;
import edu.hbmd.taotao.qingjia.pojo.ClassPojo;
import edu.hbmd.taotao.qingjia.pojo.LeaveApplicationPojo;
import edu.hbmd.taotao.qingjia.pojo.StudentPojo;
import edu.hbmd.taotao.qingjia.pojo.TeacherPojo;
import edu.hbmd.taotao.qingjia.service.BanzhurenShenPiService;
import edu.hbmd.taotao.qingjia.service.ClassService;
import edu.hbmd.taotao.qingjia.service.FudaoyuanShenPiService;
import edu.hbmd.taotao.qingjia.service.LeaveApplicationService;
import edu.hbmd.taotao.qingjia.service.StudentService;
import edu.hbmd.taotao.qingjia.service.YuanlingdaoShenPiService;
import edu.hbmd.taotao.qingjia.shiro.LoginUser;
import edu.hbmd.taotao.qingjia.util.R;
import edu.hbmd.taotao.qingjia.vo.LeaveApplicationVo;
import edu.hbmd.taotao.qingjia.vo.PageResult;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

/**
 * 请假流程接口（Vue 前端使用），覆盖菜单「请假管理」下的四个页面：
 *   我的申请   /leave/student/index     -> /api/leave/student/**
 *   班主任审批 /leave/banzhuren/index    -> /api/leave/banzhuren/**
 *   辅导员审批 /leave/fudaoyuan/index    -> /api/leave/fudaoyuan/**
 *   院领导审批 /leave/yuanlingdao/index  -> /api/leave/yuanlingdao/**
 *
 * URL 沿用老的四个 Thymeleaf controller，加 /api 前缀、返回 JSON、分页统一 PageResult。
 * 查询和审批都直接复用老 service（Banzhuren/Fudaoyuan/YuanlingdaoShenPiService），
 * 状态机不在这一层重写 —— 它只写一份，避免两个入口对「下一状态」的理解跑偏。
 *
 * 状态机（application_status 表）：
 *   1 待提交 --提交--> 2 待班主任审批
 *   班主任：同意且 type=1（一天以内） -> 5 待销假；同意且其它类型 -> 3 待辅导员审批；不同意 -> 6 审批未通过
 *   辅导员：同意且 type=2（一天以外三天以内） -> 5 待销假；同意且 type=3/4 -> 4 待院领导审批；不同意 -> 6
 *   院领导：同意 -> 5 待销假；不同意 -> 6
 *   学生销假（上传照片） -> 7 归档
 *
 * 相比老 controller 补的几处（都是权限/数据完整性问题）：
 * 1. 老 /leave/student/detail/{id}、/submitDraft/{id}、/delete/{id} 只挂了权限注解，
 *    没有校验这条申请是不是当前学生的：换个 id 就能看/改/删别人的申请。
 *    这里统一用 assertOwnApplication 兜住。
 * 2. 老 /leave/student/saveDraft 直接把请求体交给 saveOrUpdate：请求体里带上别人的
 *    申请 id 就能覆盖别人的记录。现在只允许更新「自己的、且仍是待提交状态」的记录。
 * 3. 班主任/辅导员/院领导查询时用登录教师自己的班级/学院范围；详情接口老代码不限范围，
 *    这里也按同样的范围校验（列表能看到的才看得到详情），避免越权看别的学院的学生请假。
 * 4. 审批接口不再把异常吞成 R.error(e.getMessage())：直接抛 ResultException 给全局
 *    异常处理器，前端才能拿到 51003（状态错误）、51004（申请不存在）这些业务码。
 * 5. 分页走 service 新增的 size 重载，前端选「每页 50 条」时才真的返回 50 行。
 *
 * 时间字段（begintime / endtime / xiaoJiaTime / createTime）统一按
 * application.yml 里配的 'yyyy-MM-dd HH:mm:ss' 收发，前端不用转时间戳。
 */
@RestController
@RequestMapping("/api/leave")
public class ApiLeaveController extends BaseController {

    /* 学生端权限，与 menu 表一致 */
    private static final String PERM_STUDENT_ACCESS = "leave:student:access";
    private static final String PERM_STUDENT_ADD    = "leave:student:add";
    private static final String PERM_STUDENT_DETAIL = "leave:student:detail";
    private static final String PERM_STUDENT_DELETE = "leave:student:delete";
    private static final String PERM_STUDENT_XIAOJIA = "leave:student:xiaojia";
    private static final String PERM_BANZHUREN      = "leave:banzhuren:access";
    private static final String PERM_FUDAOYUAN      = "leave:fudaoyuan:access";
    private static final String PERM_YUANLINGDAO    = "leave:yuanlingdao:access";

    /** 状态：待提交。只有这个状态可以改 / 删 / 提交 */
    private static final int STATUS_DRAFT = 1;

    /** 单页条数上限，理由同 ApiCollegeController.MAX_PAGE_SIZE */
    private static final int MAX_PAGE_SIZE = 200;
    /** PageConstant.pageSize 不是 final，注解里不能当编译期常量用 */
    private static final String DEFAULT_PAGE_SIZE = "10";

    @Autowired
    private LeaveApplicationService leaveApplicationService;
    @Autowired
    private BanzhurenShenPiService banzhurenShenPiService;
    @Autowired
    private FudaoyuanShenPiService fudaoyuanShenPiService;
    @Autowired
    private YuanlingdaoShenPiService yuanlingdaoShenPiService;
    @Autowired
    private StudentService studentService;
    @Autowired
    private ClassService classService;

    /* ==================== 我的申请（学生） ==================== */

    /**
     * 分页查询当前学生的请假申请。
     *
     * @param status 申请状态，不传则查全部（状态字典见 application_status 表）
     */
    @GetMapping("/student/index")
    @RequiresPermissions(PERM_STUDENT_ACCESS)
    @RequiresRoles("学生")
    public R studentIndex(@RequestParam(value = "current", defaultValue = "1") long current,
                          @RequestParam(value = "size", defaultValue = DEFAULT_PAGE_SIZE) long size,
                          @RequestParam(value = "status", required = false) Integer status) {
        int safeCurrent = safeCurrent(current);
        int safeSize = safeSize(size);
        Integer studentId = currentStudent().getId();

        Page<LeaveApplicationVo> page =
                leaveApplicationService.pageMyApplications(safeCurrent, safeSize, studentId, status);
        return R.ok().put("data", PageResult.of(page));
    }

    /**
     * 申请详情（含审批记录）。只能看自己的。
     */
    @GetMapping("/student/detail/{id}")
    @RequiresPermissions(PERM_STUDENT_DETAIL)
    @RequiresRoles("学生")
    public R studentDetail(@PathVariable Integer id) {
        LeaveApplicationVo vo = leaveApplicationService.getDetailById(id);
        assertOwnApplication(vo, currentStudent().getId());
        return R.ok().put("data", vo);
    }

    /**
     * 保存草稿（状态 1 待提交）。带 id 表示修改已有的草稿。
     */
    @PostMapping("/student/saveDraft")
    @RequiresPermissions(PERM_STUDENT_ADD)
    @RequiresRoles("学生")
    public R saveDraft(@Valid @RequestBody LeaveApplicationPojo application) {
        Integer studentId = currentStudent().getId();
        application.setStudentId(studentId);
        assertDraftOwnedAndEditable(application);
        return leaveApplicationService.saveDraft(application)
                ? R.ok("草稿保存成功").put("data", application)
                : R.error("保存失败");
    }

    /**
     * 新建并直接提交 / 修改后提交（状态 -> 2 待班主任审批）。
     */
    @PostMapping("/student/submit")
    @RequiresPermissions(PERM_STUDENT_ADD)
    @RequiresRoles("学生")
    public R submit(@Valid @RequestBody LeaveApplicationPojo application) {
        Integer studentId = currentStudent().getId();
        application.setStudentId(studentId);
        assertDraftOwnedAndEditable(application);
        return leaveApplicationService.submitApplication(application)
                ? R.ok("提交成功").put("data", application)
                : R.error("提交失败");
    }

    /**
     * 提交已有草稿。
     */
    @PostMapping("/student/submitDraft/{id}")
    @RequiresPermissions(PERM_STUDENT_ADD)
    @RequiresRoles("学生")
    public R submitDraft(@PathVariable Integer id) {
        assertOwnApplication(leaveApplicationService.getById(id), currentStudent().getId());
        return leaveApplicationService.submitDraft(id)
                ? R.ok("提交成功")
                : R.error("提交失败，仅待提交状态可提交");
    }

    /**
     * 删除草稿（只有待提交状态可以删）。
     */
    @DeleteMapping("/student/delete/{id}")
    @RequiresPermissions(PERM_STUDENT_DELETE)
    @RequiresRoles("学生")
    public R deleteDraft(@PathVariable Integer id) {
        assertOwnApplication(leaveApplicationService.getById(id), currentStudent().getId());
        return leaveApplicationService.deleteDraft(id)
                ? R.ok("删除成功")
                : R.error("删除失败，仅待提交状态可删除");
    }

    /**
     * 销假：上传销假照片，状态 5 待销假 -> 7 归档。
     * 照片存到 project.upload.upload-file-xiao-jia-path，返回体里带上归档后的相对路径。
     */
    @PostMapping("/student/xiaojia")
    @RequiresPermissions(PERM_STUDENT_XIAOJIA)
    @RequiresRoles("学生")
    public R xiaoJia(@RequestParam("applicationId") Integer applicationId,
                     @RequestParam("photo") MultipartFile photo) {
        StudentPojo student = currentStudent();
        assertOwnApplication(leaveApplicationService.getById(applicationId), student.getId());
        boolean success = leaveApplicationService.xiaoJia(applicationId, photo, student.getNum());
        return success ? R.ok("销假成功") : R.error("销假失败");
    }

    /* ==================== 班主任审批 ==================== */

    /**
     * 班主任分页查询自己带班学生的请假申请。
     */
    @GetMapping("/banzhuren/index")
    @RequiresPermissions(PERM_BANZHUREN)
    public R banzhurenIndex(@RequestParam(value = "current", defaultValue = "1") long current,
                            @RequestParam(value = "size", defaultValue = DEFAULT_PAGE_SIZE) long size,
                            @RequestParam(value = "status", required = false) Integer status,
                            @RequestParam(value = "classId", required = false) Integer classId) {
        int safeCurrent = safeCurrent(current);
        int safeSize = safeSize(size);

        List<ClassPojo> myClasses = myClasses();
        List<Integer> classIds = new ArrayList<>();
        if (classId != null && classId > 0) {
            // 只允许按自己带的班筛选，传别人的班 id 直接当作没筛
            if (containsClass(myClasses, classId)) {
                classIds.add(classId);
            }
        } else {
            for (ClassPojo clazz : myClasses) {
                classIds.add(clazz.getId());
            }
        }

        Page<LeaveApplicationVo> page =
                banzhurenShenPiService.pageByClassIds(safeCurrent, safeSize, classIds, status);
        return R.ok().put("data", PageResult.of(page));
    }

    @GetMapping("/banzhuren/detail/{id}")
    @RequiresPermissions(PERM_BANZHUREN)
    public R banzhurenDetail(@PathVariable Integer id) {
        LeaveApplicationVo vo = banzhurenShenPiService.getDetail(id);
        assertStudentInClasses(vo, myClasses());
        return R.ok().put("data", vo);
    }

    /**
     * 班主任带班的班级下拉。
     *
     * 单独开这个接口而不是复用 /api/class/list：那条路径要 baseinfo:class:access 权限，
     * 而「班主任」角色的菜单里并没有「班级管理」，直接用会 403。
     * 这里的数据范围就是 class.banZhuRenId = 登录教师，天然不会越权。
     */
    @GetMapping("/banzhuren/classes")
    @RequiresPermissions(PERM_BANZHUREN)
    public R banzhurenClasses() {
        return R.ok().put("data", myClasses());
    }

    /**
     * 班主任审批。result：1 同意 / 2 不同意。
     * 审批人固定取登录教师，不接受前端传 teacherId（否则谁都能冒名审批）。
     */
    @PostMapping("/banzhuren/approve")
    @RequiresPermissions(PERM_BANZHUREN)
    public R banzhurenApprove(@RequestBody ApproveForm form) {
        if (form == null || form.getApplicationId() == null) {
            return R.error(400, "applicationId 不能为空");
        }
        Integer result = checkResult(form.getResult());
        assertStudentInClasses(banzhurenShenPiService.getDetail(form.getApplicationId()), myClasses());
        return banzhurenShenPiService.approve(form.getApplicationId(), currentTeacher().getId(),
                result, form.getOpinion())
                ? R.ok("审批成功")
                : R.error("审批失败");
    }

    /* ==================== 辅导员审批 ==================== */

    @GetMapping("/fudaoyuan/index")
    @RequiresPermissions(PERM_FUDAOYUAN)
    public R fudaoyuanIndex(@RequestParam(value = "current", defaultValue = "1") long current,
                            @RequestParam(value = "size", defaultValue = DEFAULT_PAGE_SIZE) long size,
                            @RequestParam(value = "status", required = false) Integer status,
                            @RequestParam(value = "grade", required = false) Integer grade,
                            @RequestParam(value = "classId", required = false) Integer classId) {
        int safeCurrent = safeCurrent(current);
        int safeSize = safeSize(size);
        Integer collegeId = myCollegeId();

        Page<LeaveApplicationVo> page = fudaoyuanShenPiService.pageByCollege(
                safeCurrent, safeSize, collegeId, grade, classId, status);
        return R.ok().put("data", PageResult.of(page));
    }

    /**
     * 年级联动班级下拉，固定按登录教师所在学院 + 传入的年级查。
     */
    @GetMapping("/fudaoyuan/classes")
    @RequiresPermissions(PERM_FUDAOYUAN)
    public R fudaoyuanClasses(@RequestParam(value = "grade", required = false) Integer grade) {
        List<ClassPojo> classes = classService.listByCollegeAndGrade(myCollegeId(), grade);
        return R.ok().put("data", classes);
    }

    @GetMapping("/fudaoyuan/detail/{id}")
    @RequiresPermissions(PERM_FUDAOYUAN)
    public R fudaoyuanDetail(@PathVariable Integer id) {
        LeaveApplicationVo vo = fudaoyuanShenPiService.getDetail(id);
        assertStudentInCollege(vo, myCollegeId());
        return R.ok().put("data", vo);
    }

    @PostMapping("/fudaoyuan/approve")
    @RequiresPermissions(PERM_FUDAOYUAN)
    public R fudaoyuanApprove(@RequestBody ApproveForm form) {
        if (form == null || form.getApplicationId() == null) {
            return R.error(400, "applicationId 不能为空");
        }
        Integer result = checkResult(form.getResult());
        assertStudentInCollege(fudaoyuanShenPiService.getDetail(form.getApplicationId()), myCollegeId());
        return fudaoyuanShenPiService.approve(form.getApplicationId(), currentTeacher().getId(),
                result, form.getOpinion())
                ? R.ok("审批成功")
                : R.error("审批失败");
    }

    /* ==================== 院领导审批 ==================== */

    @GetMapping("/yuanlingdao/index")
    @RequiresPermissions(PERM_YUANLINGDAO)
    public R yuanlingdaoIndex(@RequestParam(value = "current", defaultValue = "1") long current,
                              @RequestParam(value = "size", defaultValue = DEFAULT_PAGE_SIZE) long size,
                              @RequestParam(value = "status", required = false) Integer status,
                              @RequestParam(value = "grade", required = false) Integer grade,
                              @RequestParam(value = "classId", required = false) Integer classId) {
        int safeCurrent = safeCurrent(current);
        int safeSize = safeSize(size);
        Integer collegeId = myCollegeId();

        Page<LeaveApplicationVo> page = yuanlingdaoShenPiService.pageByCollege(
                safeCurrent, safeSize, collegeId, grade, classId, status);
        return R.ok().put("data", PageResult.of(page));
    }

    @GetMapping("/yuanlingdao/classes")
    @RequiresPermissions(PERM_YUANLINGDAO)
    public R yuanlingdaoClasses(@RequestParam(value = "grade", required = false) Integer grade) {
        List<ClassPojo> classes = classService.listByCollegeAndGrade(myCollegeId(), grade);
        return R.ok().put("data", classes);
    }

    @GetMapping("/yuanlingdao/detail/{id}")
    @RequiresPermissions(PERM_YUANLINGDAO)
    public R yuanlingdaoDetail(@PathVariable Integer id) {
        LeaveApplicationVo vo = yuanlingdaoShenPiService.getDetail(id);
        assertStudentInCollege(vo, myCollegeId());
        return R.ok().put("data", vo);
    }

    @PostMapping("/yuanlingdao/approve")
    @RequiresPermissions(PERM_YUANLINGDAO)
    @RequiresRoles("学院领导")
    public R yuanlingdaoApprove(@RequestBody ApproveForm form) {
        if (form == null || form.getApplicationId() == null) {
            return R.error(400, "applicationId 不能为空");
        }
        Integer result = checkResult(form.getResult());
        assertStudentInCollege(yuanlingdaoShenPiService.getDetail(form.getApplicationId()), myCollegeId());
        return yuanlingdaoShenPiService.approve(form.getApplicationId(), currentTeacher().getId(),
                result, form.getOpinion())
                ? R.ok("审批成功")
                : R.error("审批失败");
    }

    /* ==================== 内部工具 ==================== */

    private int safeCurrent(long current) {
        return current < 1 ? 1 : (int) Math.min(current, Integer.MAX_VALUE);
    }

    private int safeSize(long size) {
        return size < 1 ? PageConstant.pageSize : (int) Math.min(size, MAX_PAGE_SIZE);
    }

    private StudentPojo currentStudent() {
        LoginUser loginUser = getLoginUser();
        if (loginUser == null || loginUser.getStudent() == null) {
            throw new ResultException(ResultEnum.LEAVE_APPLIICATION_NO_PERMISSION.getStatus(),
                    "当前登录用户不是学生，无法操作请假申请");
        }
        return loginUser.getStudent();
    }

    private TeacherPojo currentTeacher() {
        LoginUser loginUser = getLoginUser();
        if (loginUser == null || loginUser.getTeacher() == null) {
            throw new ResultException(ResultEnum.LEAVE_APPLIICATION_NO_PERMISSION.getStatus(),
                    "当前登录用户不是教师，无法审批");
        }
        return loginUser.getTeacher();
    }

    private Integer myCollegeId() {
        return currentTeacher().getCollegeId();
    }

    /** 当前教师担任班主任的班级列表 */
    private List<ClassPojo> myClasses() {
        return classService.listByTeacherId(currentTeacher().getId());
    }

    private boolean containsClass(List<ClassPojo> classes, Integer classId) {
        for (ClassPojo clazz : classes) {
            if (classId.equals(clazz.getId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 校验这条申请属于当前学生。
     * 老代码的 detail / submitDraft / delete 只挂了权限、没校验归属，
     * 换个 id 就能看/改/删别人的申请。
     */
    private void assertOwnApplication(LeaveApplicationPojo application, Integer studentId) {
        if (application == null) {
            throw new ResultException(ResultEnum.LEAVE_APPLIICATION_NOT_EXIST);
        }
        if (application.getStudentId() == null || !application.getStudentId().equals(studentId)) {
            throw new ResultException(ResultEnum.LEAVE_APPLIICATION_NO_PERMISSION);
        }
    }

    /**
     * 保存/提交前的校验：带 id 时必须是「自己的、且还在待提交状态」的记录。
     * 不带 id 就是新增，直接放过。
     */
    private void assertDraftOwnedAndEditable(LeaveApplicationPojo application) {
        if (application.getId() == null) {
            return;
        }
        LeaveApplicationPojo exist = leaveApplicationService.getById(application.getId());
        assertOwnApplication(exist, application.getStudentId());
        if (exist.getStatus() == null || exist.getStatus() != STATUS_DRAFT) {
            throw new ResultException(ResultEnum.LEAVE_APPLIICATION_STATUS_ERROR);
        }
    }

    /** 班主任范围校验：申请对应的学生必须在当前教师带的班里 */
    private void assertStudentInClasses(LeaveApplicationVo vo, List<ClassPojo> classes) {
        StudentPojo student = studentOf(vo);
        if (!containsClass(classes, student.getClassId())) {
            throw new ResultException(ResultEnum.LEAVE_APPLIICATION_NO_PERMISSION);
        }
    }

    /** 辅导员 / 院领导范围校验：申请对应的学生必须在本学院 */
    private void assertStudentInCollege(LeaveApplicationVo vo, Integer collegeId) {
        if (collegeId == null) {
            throw new ResultException(ResultEnum.LEAVE_APPLIICATION_NO_PERMISSION.getStatus(),
                    "当前教师没有所属学院，无法审批");
        }
        StudentPojo student = studentOf(vo);
        ClassPojo clazz = classService.getById(student.getClassId());
        if (clazz == null || !collegeId.equals(clazz.getBelongToCollegeId())) {
            throw new ResultException(ResultEnum.LEAVE_APPLIICATION_NO_PERMISSION);
        }
    }

    private StudentPojo studentOf(LeaveApplicationVo vo) {
        if (vo == null) {
            throw new ResultException(ResultEnum.LEAVE_APPLIICATION_NOT_EXIST);
        }
        StudentPojo student = studentService.getById(vo.getStudentId());
        if (student == null) {
            throw new ResultException(ResultEnum.STUDENT_NOT_EXIST);
        }
        return student;
    }

    /** 审批结果只允许 1（同意）/ 2（不同意），挡住前端传 0/3 之类的脏值 */
    private Integer checkResult(Integer result) {
        if (result == null || (result != 1 && result != 2)) {
            throw new ResultException(400, "审批结果只能传 1（同意）或 2（不同意）");
        }
        return result;
    }

    /**
     * 审批请求体。teacherId 故意不接收 —— 审批人一律取登录教师，
     * 否则前端传谁的 id 就记在谁头上。
     */
    public static class ApproveForm {
        private Integer applicationId;
        /** 1 同意 / 2 不同意 */
        private Integer result;
        private String opinion;

        public Integer getApplicationId() {
            return applicationId;
        }

        public void setApplicationId(Integer applicationId) {
            this.applicationId = applicationId;
        }

        public Integer getResult() {
            return result;
        }

        public void setResult(Integer result) {
            this.result = result;
        }

        public String getOpinion() {
            return opinion;
        }

        public void setOpinion(String opinion) {
            this.opinion = opinion;
        }
    }
}
