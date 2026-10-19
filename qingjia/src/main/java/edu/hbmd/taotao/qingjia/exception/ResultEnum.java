package edu.hbmd.taotao.qingjia.exception;

import lombok.Getter;

@Getter
public enum ResultEnum {
    /**
     * 通用状态
     */
    SUCCESS(200, "成功"),
    ERROR(500, "错误"),

    /**
     * 登录模块01 模块内部编号**
     */
    LOGIN_STUDENT_NUM_NO_EXIST(50101, "学号不存在"),
    LOGIN_TEACHER_NUM_NO_EXIST(50102, "工号不存在"),
    LOGIN_PWD_ERROR(50103, "密码不正确"),
    LOGIN_USER_CAPTCHA_ERROR(50104, "验证码错误"),

    /**
     * 学院模块02
     */
    COLLEGE_ADD_UPDATE_SAME_NAME(50201,"你输入的学院名或者学院编号已存在"),
    COLLEGE_NOT_EXIST(50202,"学院不存在"),
    COLLEGE_HAS_CLASS(50203,"该学院下还有班级，请先删除或转移这些班级"),
    COLLEGE_HAS_TEACHER(50204,"该学院下还有教师，请先删除或转移这些教师"),
    /**
     * 系部模块03
     */
    /**
     * 专业模块04
     */
    /**
     * 班级模块05
     */
    ClASS_ADD_UPDATE_SAME_NAME(50501,"你输入的班级编号已存在"),
    CLASS_NOT_EXIST(50502,"班级不存在"),
    CLASS_BANZHUREN_ROLE_NOT_EXIST(50503,"指定班级班主任时，班主任角色不存在"),
    CLASS_HAS_STUDENT(50504,"该班级下还有学生，请先删除或转移这些学生"),

    /**
     * 教师模块06
     */
    TEACHER_NUM_EXIST(50601,"教师工号已存在"),
    TEACHER_NOT_EXIST(50602,"教师不存在"),
    TEACHER_IS_BANZHUREN(50603,"该教师还是某个班的班主任，请先解除班主任关系"),
    /**
     * 学生模块07
     */
    STUDENT_NUM_EXIST(50701,"学号已存在"),
    STUDENT_NOT_EXIST(50702,"学生不存在"),
    /**
     * 角色模块08
     */
    ROLE_ADD_SAME_ROLNAME(50801,"你添加的角色名已存在！"),
    ROLE_NOT_EXIST(50802,"角色不存在"),
    ROLE_HAS_USER(50803,"该角色已分配给用户，请先解除分配"),

    /**
     * 菜单/权限模块09
     */
    MENU_NOT_EXIST(50901,"菜单不存在"),
    MENU_HAS_CHILDREN(50902,"该菜单下还有子菜单，请先删除子菜单"),
    MENU_SAME_NAME(50903,"同级下已存在同名菜单"),

    /**
     * 权限模块09
     */
    /**
     * 请假模块10
     */
    LEAVE_APPLIICATION_NO_PERMISSION(51001,"无权限操作"),
    LEAVE_APPLIICATION_NOT_SUBMIT(51002,"只有待提交状态的申请可提交"),
    LEAVE_APPLIICATION_STATUS_ERROR(51003,"请假申请状态错误"),
    LEAVE_APPLIICATION_NOT_EXIST(51004,"请假申请不存在"),
    EAVE_APPLIICATION_BANZHUREN_SHENPI_SAVE_ERROR(51005,"班主任审批记录保存失败"),
    EAVE_APPLIICATION_FUDAOYUAN_SHENPI_SAVE_ERROR(51006,"辅导员审批记录保存失败"),
    EAVE_APPLIICATION_YUANLIDAO_SHENPI_SAVE_ERROR(51007,"学院领导审批记录保存失败"),
    EAVE_APPLIICATION_XIAO_JIA_PHOTO_NOT_EXIST(51008,"销假时没有提交照片"),
    ;
    private Integer status;//状态码
    private String message;//提示信息

    ResultEnum(Integer code, String message) {
        this.status = code;
        this.message = message;
    }

}
