package edu.hbmd.taotao.qingjia.common;

import edu.hbmd.taotao.qingjia.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.servlet.ServletContext;

@Component
public class DataInitializer implements CommandLineRunner {
    @Autowired
    ServletContext servletContext;
    @Autowired
    CollegeService collegeService;
    @Autowired
    GradeService gradeService;
    @Autowired
    AproveTypeService aproveTypeService;
    @Autowired
    LeaveApplicationTypeService leaveApplicationTypeService;
    @Autowired
    MenuTypeService menuTypeService;
    @Autowired
    RoleTypeService roleTypeService;
    @Autowired
    SexTypeService sexTypeService;
    @Autowired
    ApplicationStatusService applicationStatusService;

    public static final String COLLEGES= "colleges";
    public static final String GRADES= "grades";
    public static final String APROVE_TYPES = "aproveTypes";
    public static final String LEAVE_APPLICATION_TYPES = "leaveApplicationTypes";
    public static final String MENY_TYPES= "menuTypes";
    public static final String ROLE_TYPES = "roleTypes";
    public static final String SEX_TYPES= "sexTypes";
    public static final String APPLICATION_STATUS= "applicationStatus";
    @Override
    public void run(String... args) throws Exception {
        //加载学院
        loadColleges();
        //加载年级
        loadGrades();
        // 审批类型
        loadAproveType();
        //请假申请类型
        loadLeaveApplicationType();
        //菜单类型
        loadMenuType();
        //角色类型
        loadRoleType();
        //性别类型
        loadSexType();
        //加载请假申请状态
        LoadApplicationStatuses();
    }


    public void loadColleges() {
        servletContext.setAttribute(COLLEGES,collegeService.getAll());
    }

    public void loadGrades() {
        servletContext.setAttribute(GRADES,gradeService.getAll());
    }

    // 审批类型
    public void loadAproveType() {
        servletContext.setAttribute(APROVE_TYPES,aproveTypeService.getAll());
    }
    //请假申请类型
    public void loadLeaveApplicationType(){
        servletContext.setAttribute(LEAVE_APPLICATION_TYPES,leaveApplicationTypeService.getAll());
    }
    //菜单类型
    public void loadMenuType(){
        servletContext.setAttribute(MENY_TYPES,menuTypeService.getAll());
    }
    //角色类型
    public void loadRoleType(){
        servletContext.setAttribute(ROLE_TYPES,roleTypeService.getAll());
    }
    //性别类型
    public void loadSexType(){
        servletContext.setAttribute(SEX_TYPES,sexTypeService.getAll());
    }
    //加载请假申请状态
    private void LoadApplicationStatuses() {
        servletContext.setAttribute(APPLICATION_STATUS,applicationStatusService.list());
    }
}
