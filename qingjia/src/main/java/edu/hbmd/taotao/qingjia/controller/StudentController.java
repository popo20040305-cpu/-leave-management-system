package edu.hbmd.taotao.qingjia.controller;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.hbmd.taotao.qingjia.constant.RoleTypeConstant;
import edu.hbmd.taotao.qingjia.pojo.ClassPojo;
import edu.hbmd.taotao.qingjia.pojo.CollegePojo;
import edu.hbmd.taotao.qingjia.pojo.GradePojo;
import edu.hbmd.taotao.qingjia.pojo.StudentPojo;
import edu.hbmd.taotao.qingjia.properties.UploadFileProperties;
import edu.hbmd.taotao.qingjia.service.StudentService;
import edu.hbmd.taotao.qingjia.shiro.LoginUser;
import edu.hbmd.taotao.qingjia.util.R;
import edu.hbmd.taotao.qingjia.vo.StudentVo;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/student")
public class StudentController extends BaseController {
    @Autowired
    StudentService studentService;
    @Autowired
    UploadFileProperties uploadFileProperties;

    /**
     * 学生管理首页
     */
    @GetMapping("/index")
    public String index(Model model) {
        // 获取当前用户可见的学院列表
        List<CollegePojo> colleges = getCollegeByLoginUser();
        model.addAttribute(BaseController.COLLEGES, colleges);

        // 获取所有年级（用于查询条件和表单下拉）
        List<GradePojo> grades = getGradesByLoginUser();
        model.addAttribute(BaseController.GRADES, grades);
        // 获取所有班级（用于查询条件和表单下拉）
        //model.addAttribute("classes", getClassesByLoginUser(null,null));
        StudentVo student = new StudentVo();
        if (colleges.size()>0){
            student.setCollegeId(colleges.get(0).getId());
        }
        if (grades.size()>0){
            student.setGrade(grades.get(0).getGrade());
        }
        // 空分页对象，避免模板报错
        model.addAttribute("page", new Page<>());
        model.addAttribute("studentQuery", student);
        return "student/studentes";
    }


    /**
     * 根据学院ID和年级获取班级列表（AJAX）
     */
    @GetMapping("/getClasses")
    @ResponseBody
    public R getClasses(@RequestParam(required = false) Integer collegeId,
                        @RequestParam(required = false) Integer grade) {
        List<ClassPojo> classList = getClassesByLoginUser(collegeId,grade);
        return R.ok().put("data", classList);
    }
    /**
     * 分页查询（条件查询）
     */
    @GetMapping("/query")
    @RequiresPermissions("baseinfo:student:query")
    public String query(StudentVo student,
                        @RequestParam(defaultValue = "1") Integer curPage,
                        Model model) {
        LoginUser loginUser = getLoginUser();
        if (loginUser.getRoleType() == RoleTypeConstant.ROLE_TYPE_PERSON&&loginUser.getType()==LoginUser.TYPE_STUDENT){
            student.setId(loginUser.getStudent().getId());
        }
        Page<StudentVo> page = studentService.pageStudents(curPage, student);
        model.addAttribute("page", page);
        model.addAttribute("studentQuery", student);

        // 重新加载下拉数据，以便回显
        List<CollegePojo> collegeList = getCollegeByLoginUser();
        model.addAttribute(BaseController.COLLEGES, collegeList);
        // 获取所有年级（用于查询条件和表单下拉）
        model.addAttribute(BaseController.GRADES, getGradesByLoginUser());
        List<ClassPojo> classList = classService.listByCollegeAndGrade(student.getCollegeId(),student.getGrade());
        model.addAttribute("classes", classList);

        return "student/studentes";
    }



    /* 保存学生（新增或修改）
            */
    @PostMapping("/save")
    @ResponseBody
    @RequiresPermissions(value = {"baseinfo:student:add", "baseinfo:student:update"}, logical = Logical.OR)
    public R save(@RequestParam("student") String studentJson,
                  @RequestParam(value = "photo", required = false) MultipartFile photo) {
        StudentPojo studentPojo = JSONUtil.toBean(studentJson, StudentPojo.class);

        if (!studentService.checkNumUnique(studentPojo)){
            return R.error("学号"+studentPojo.getNum()+"已存在");
        }
        // 处理头像上传
        if (photo != null && !photo.isEmpty()) {
            try {
                // 如果是修改，删除旧头像
                if (studentPojo.getId() != null) {
                    StudentPojo old = studentService.getById(studentPojo.getId());
                    if (old != null && old.getAvatarUrl() != null) {
                        String oldPath = uploadFileProperties.getUploadFileStudentPath()
                                + old.getAvatarUrl().replace("/studentPhoto/", "");
                        File oldFile = new File(oldPath);
                        if (oldFile.exists()) {
                            oldFile.delete();
                        }
                    }
                }

                String ext = photo.getOriginalFilename().substring(photo.getOriginalFilename().lastIndexOf("."));
                String newFileName = studentPojo.getNum() + "_" + System.currentTimeMillis() + ext;
                File dir = new File(uploadFileProperties.getUploadFileStudentPath());
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                File dest = new File(dir, newFileName);
                photo.transferTo(dest);
                String photoUrl = "/studentPhoto/" + newFileName;
                studentPojo.setAvatarUrl(photoUrl);
            } catch (IOException e) {
                return R.error("照片上传失败：" + e.getMessage());
            }
        } else {
            // 未上传新照片，若是修改则保留原照片
            if (studentPojo.getId() != null) {
                StudentPojo old = studentService.getById(studentPojo.getId());
                if (old != null) {
                    studentPojo.setAvatarUrl(old.getAvatarUrl());
                }
            }
        }

        if (studentPojo.getId() != null) {
            // 修改
            boolean success = studentService.saveStudent(studentPojo);
            return success ? R.ok("修改成功") : R.error("修改失败");
        } else {
            // 新增
            if (studentPojo.getPassword() == null || studentPojo.getPassword().isEmpty()) {
                return R.error("密码不能为空");
            }
            boolean success = studentService.saveStudent(studentPojo);
            return success ? R.ok("添加成功") : R.error("添加失败");
        }
    }

    /**
     * 获取单个学生信息（用于修改回显）
     */
    @GetMapping("/info/{id}")
    @ResponseBody
    @RequiresPermissions("baseinfo:student:query")
    public R info(@PathVariable Integer id) {
        StudentVo vo = studentService.selectById(id);
        if (vo == null) {
            return R.error("学生不存在");
        }
        return R.ok().put("data", vo);
    }
    /**
     * 重置学生密码
     */
    @PostMapping("/resetPassword")
    @ResponseBody
    @RequiresPermissions("baseinfo:student:updatepassword")
    public R resetPassword(@RequestParam Integer studentId, @RequestParam String newPassword) {
        StudentPojo student = studentService.getById(studentId);
        if (student == null) {
            return R.error("学生不存在");
        }
        if (newPassword == null || newPassword.length() < 6) {
            return R.error("密码长度不能少于6位");
        }
        boolean success = studentService.resetPassword(studentId, newPassword);
        return success ? R.ok("密码重置成功") : R.error("密码重置失败");
    }

}
