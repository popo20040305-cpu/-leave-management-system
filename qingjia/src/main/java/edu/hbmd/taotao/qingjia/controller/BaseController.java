package edu.hbmd.taotao.qingjia.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.hbmd.taotao.qingjia.api.ApiPageSizeController;
import edu.hbmd.taotao.qingjia.common.DataInitializer;
import edu.hbmd.taotao.qingjia.constant.PageConstant;
import edu.hbmd.taotao.qingjia.constant.RoleTypeConstant;
import edu.hbmd.taotao.qingjia.pojo.*;
import edu.hbmd.taotao.qingjia.service.ClassService;
import edu.hbmd.taotao.qingjia.service.CollegeService;
import edu.hbmd.taotao.qingjia.service.GradeService;
import edu.hbmd.taotao.qingjia.shiro.LoginUser;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import javax.servlet.http.HttpServletRequest;

import javax.servlet.ServletContext;
import java.util.ArrayList;
import java.util.List;

@Controller
public class BaseController {
    @Autowired
    ServletContext servletContext;
    @Autowired
    ClassService classService;
    @Autowired
    CollegeService collegeService;
    @Autowired
    GradeService gradeService;
    @Autowired
    protected HttpServletRequest request;

    public static final String COLLEGES = "colleges";
    public static final String GRADES = "grades";
    public static final String APROVE_TYPES = "aproveTypes";
    public static final String LEAVE_APPLICATION_TYPES = "leaveApplicationTypes";
    public static final String MENY_TYPES = "menuTypes";
    public static final String ROLE_TYPES = "roleTypes";
    public static final String SEX_TYPES = "sexTypes";
    // 在 BaseController 中添加常量
    public static final String APPLICATION_STATUSES = "applicationStatuses";

    protected List<CollegePojo> getColleges() {
        return (List<CollegePojo>) servletContext.getAttribute(DataInitializer.COLLEGES);
    }

    protected List<GradePojo> getGrades() {
        return (List<GradePojo>) servletContext.getAttribute(DataInitializer.GRADES);
    }

    protected List<AproveTypePojo> getAproveTypes() {
        return (List<AproveTypePojo>) servletContext.getAttribute(DataInitializer.APROVE_TYPES);
    }

    protected List<LeaveApplicationTypePojo> getleaveApplicationTypes() {
        return (List<LeaveApplicationTypePojo>) servletContext.getAttribute(DataInitializer.LEAVE_APPLICATION_TYPES);
    }

    protected List<MenuTypePojo> getMenuTypes() {
        return (List<MenuTypePojo>) servletContext.getAttribute(DataInitializer.MENY_TYPES);
    }

    protected List<RoleTypePojo> getRoleTypes() {
        return (List<RoleTypePojo>) servletContext.getAttribute(DataInitializer.ROLE_TYPES);
    }

    protected List<SexTypePojo> getSexTypes() {
        return (List<SexTypePojo>) servletContext.getAttribute(DataInitializer.SEX_TYPES);
    }

    protected List<ApplicationStatusPojo> getApplicationStatuses(){
        return (List<ApplicationStatusPojo>) servletContext.getAttribute(DataInitializer.APPLICATION_STATUS);
    }
    //根据登录用户类型查询学院，作为查询条件使用
    protected List<CollegePojo> getCollegeByLoginUser() {
        if (GetLoginUserType() == RoleTypeConstant.ROLE_TYPE_ALL) {
            return getColleges();
        } else {
            List<CollegePojo> colleges = new ArrayList<>();
            Integer collegeId = null;
            if (getLoginUser().getType() == LoginUser.TYPE_STUDENT) {
                ClassPojo classPojo = classService.getById(getLoginUser().getStudent().getClassId());
                CollegePojo collegePojo = collegeService.getById(classPojo.getBelongToCollegeId());
                colleges.add(collegePojo);
            } else {
                TeacherPojo teacherPojo = getLoginUser().getTeacher();
                for (CollegePojo college : getColleges()) {
                    if (college.getId() == teacherPojo.getCollegeId()) {
                        colleges.add(college);
                        break;
                    }
                }
            }
            return colleges;
        }
    }

    /**
     * 和 {@link #getCollegeByLoginUser()} 完全相同的过滤规则，但直接查库，
     * 不读 ServletContext 缓存。给前后端分离的 /api/** 接口用。
     *
     * 为什么不用缓存：那份缓存只在启动时由 DataInitializer 写一次，
     * 全项目没有任何地方在学院增删改之后刷新它，所以老页面新增一个学院
     * 要重启才看得到。新接口不该继承这个问题。
     *
     * 顺带修掉原方法里的一个 bug：college.getId() == teacherPojo.getCollegeId()
     * 是两个 Integer 做 == 比较，比的是引用。id 超出 Integer 缓存范围（127）之后
     * 就永远不相等，教师会查不到自己所在的学院。这里改用 equals。
     */
    protected List<CollegePojo> getCollegesByLoginUserFromDb() {
        LoginUser loginUser = getLoginUser();
        if (loginUser == null || GetLoginUserType() == RoleTypeConstant.ROLE_TYPE_ALL) {
            return collegeService.getAll();
        }
        List<CollegePojo> colleges = new ArrayList<>();
        if (loginUser.getType() == LoginUser.TYPE_STUDENT) {
            if (loginUser.getStudent() == null) {
                return colleges;
            }
            ClassPojo classPojo = classService.getById(loginUser.getStudent().getClassId());
            if (classPojo == null || classPojo.getBelongToCollegeId() == null) {
                return colleges;
            }
            CollegePojo collegePojo = collegeService.getById(classPojo.getBelongToCollegeId());
            if (collegePojo != null) {
                colleges.add(collegePojo);
            }
            return colleges;
        }
        if (loginUser.getType() == LoginUser.TYPE_TEACHER && loginUser.getTeacher() != null) {
            Integer collegeId = loginUser.getTeacher().getCollegeId();
            if (collegeId == null) {
                return colleges;
            }
            for (CollegePojo college : collegeService.getAll()) {
                if (collegeId.equals(college.getId())) {
                    colleges.add(college);
                    break;
                }
            }
            return colleges;
        }
        return collegeService.getAll();
    }

    //获取登录用户

    protected LoginUser getLoginUser() {
        return (LoginUser) SecurityUtils.getSubject().getPrincipal();
    }

    //获取登录类型
    protected int GetLoginUserType() {
        LoginUser loginUser = getLoginUser();
        return loginUser.getRoleType();
    }

    //根据登录用户类型查询年级，作为查询条件使用
    public String getStudentNumByLoginUser() {
        if (getLoginUser().getType() == LoginUser.TYPE_STUDENT) {
            return getLoginUser().getStudent().getNum();
        } else {
            return "";
        }
    }
    //根据登录用户类型查询年级，作为查询条件使用
    protected List<GradePojo> getGradesByLoginUser() {
        if (GetLoginUserType() == RoleTypeConstant.ROLE_TYPE_ALL || GetLoginUserType() == RoleTypeConstant.ROLE_TYPE_COLLEGE
                ||getLoginUser().getType()==LoginUser.TYPE_TEACHER) { //如果登录用户类型为全部、学院则返回全部年级
            return getGrades();
        } else {
            //登录用户类型为班级或者个人且登录用户为学生，则返回学生所在的年级
            StudentPojo student = getLoginUser().getStudent();
            ClassPojo classPojo = classService.getById(student.getClassId());
            LambdaQueryWrapper<GradePojo> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(GradePojo::getGrade, classPojo.getGrade())
                    .orderByDesc(GradePojo::getGrade);
            return gradeService.list(wrapper);

        }
    }
    /*根据登录用户、学院id、年级查询班级
     * collegeId 学院id
     *grade    年级
     */
    protected List<ClassPojo> getClassesByLoginUser(Integer collegeId,Integer grade){
        if (GetLoginUserType() == RoleTypeConstant.ROLE_TYPE_ALL||GetLoginUserType() == RoleTypeConstant.ROLE_TYPE_COLLEGE
                ||getLoginUser().getType() == LoginUser.TYPE_TEACHER){
//如果登录用户类型为全部、学院则根据学院与年级查询班级
            return classService.listByCollegeAndGrade(collegeId,grade);
        }else {
            List<ClassPojo> classes = new ArrayList<ClassPojo>();
//如果登录用户类型为班级、个人，登录用户为学生，返回学生所在的班级
            StudentPojo student = getLoginUser().getStudent();
            ClassPojo classPojo = classService.getById(student.getClassId());
            classes.add(classPojo);
            return classes;
        }
    }
    /**
     * 获取分页大小（每页条数），优先级从高到低：
     * 1. 请求参数 size / pageSize / limit（前端显式传了就以传的为准，和 usePageTable 的 size 对齐）
     * 2. 该登录用户通过 /api/setPageSize 记住的值（文档 10.2；存的是进程内缓存，
     *    不是 Shiro session —— Bearer token 模式下 session 不跨请求，见 ApiPageSizeController 注释）
     * 3. 默认 10 条
     *
     * 原实现只认请求参数，用户换个页面就丢了偏好；加这一档后，
     * 「每页 50 条」在登录期间对所有列表页生效。
     */
    protected int getPageSize() {
        // 1. 请求参数
        String pageSizeStr = request.getParameter("size");
        if (pageSizeStr == null || pageSizeStr.trim().isEmpty()) {
            pageSizeStr = request.getParameter("pageSize");
        }
        if (pageSizeStr == null || pageSizeStr.trim().isEmpty()) {
            pageSizeStr = request.getParameter("limit");
        }
        if (pageSizeStr != null && !pageSizeStr.trim().isEmpty()) {
            try {
                return Integer.parseInt(pageSizeStr.trim());
            } catch (NumberFormatException e) {
                // 传的不是数字，继续走 session / 默认值
            }
        }

        // 2. 该登录用户记住的值（/api/setPageSize 写入的进程内缓存）
        try {
            Integer remembered = ApiPageSizeController.getPageSizeOfCurrentUser();
            if (remembered != null && remembered > 0) {
                return remembered;
            }
        } catch (Exception ignored) {
            // 拿不到（比如尚未登录）不算错误，退回默认值
        }

        // 3. 默认值
        return PageConstant.pageSize;
    }

}
