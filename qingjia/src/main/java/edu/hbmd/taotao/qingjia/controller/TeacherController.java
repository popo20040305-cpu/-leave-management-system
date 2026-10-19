package edu.hbmd.taotao.qingjia.controller;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.hbmd.taotao.qingjia.constant.UserTypeConstant;
import edu.hbmd.taotao.qingjia.exception.ResultEnum;
import edu.hbmd.taotao.qingjia.exception.ResultException;
import edu.hbmd.taotao.qingjia.pojo.CollegePojo;
import edu.hbmd.taotao.qingjia.pojo.RolePojo;
import edu.hbmd.taotao.qingjia.pojo.TeacherPojo;
import edu.hbmd.taotao.qingjia.properties.UploadFileProperties;
import edu.hbmd.taotao.qingjia.service.RoleMenuService;
import edu.hbmd.taotao.qingjia.service.RoleService;
import edu.hbmd.taotao.qingjia.service.TeacherService;
import edu.hbmd.taotao.qingjia.service.UserRoleService;
import edu.hbmd.taotao.qingjia.util.R;
import edu.hbmd.taotao.qingjia.vo.TeacherVo;
import lombok.SneakyThrows;
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
@RequestMapping("/teacher")
public class TeacherController extends BaseController{
    @Autowired
    TeacherService teacherService;
    @Autowired
    UploadFileProperties uploadFileProperties;

    @Autowired
    RoleService roleService;

    @Autowired
    UserRoleService userRoleService;

    @GetMapping("/index")
    public String index(Model model){
        model.addAttribute(BaseController.COLLEGES,getCollegeByLoginUser());

        // 传入空的分页对象，避免模板报错
        model.addAttribute("page", new Page<>());
        return "teacher/teacher";
    }
    /**
     * 分页查询教师数据（form表单提交，返回页面）
     */
    @GetMapping("/query")
    @RequiresPermissions("baseinfo:teacher:qurey")
    public String query(TeacherPojo teacher,
                        @RequestParam(defaultValue = "1") Integer curPage,
                        Model model) {
        Page<TeacherVo> page = teacherService.pageTeachers(curPage, teacher);
        model.addAttribute("page", page);
        model.addAttribute("teacherQuery", teacher);
        // 学院列表（用于查询条件下拉框回显）
        List<CollegePojo> collegeList = getCollegeByLoginUser();
        model.addAttribute(BaseController.COLLEGES, collegeList);
        return "teacher/teacher";
    }
    @PostMapping("/save")
    @ResponseBody
    @RequiresPermissions(value = {"baseinfo:teacher:add", "baseinfo:teacher:update"}, logical = Logical.OR)
    public R save(@RequestParam("teacher") String teacherJson,
                  @RequestParam(value = "photo", required = false) MultipartFile photo) {
        System.out.println(teacherJson);
        TeacherPojo teacherPojo = JSONUtil.toBean(teacherJson, TeacherPojo.class);

        // 1. 先处理文件（如果上传了新照片）
        if (photo != null && !photo.isEmpty()) {
            try {
                // 如果是修改，先删除旧照片（根据数据库中的旧路径）
                if (teacherPojo.getId() != null) {
                    TeacherPojo oldTeacher = teacherService.getById(teacherPojo.getId());
                    if (oldTeacher != null && oldTeacher.getAvatarUrl() != null) {
                        // 从 avatarUrl 中提取文件名（如 /teacherPhoto/xxx.jpg）
                        String oldPath = uploadFileProperties.getUploadFileTeacherPath()
                                + oldTeacher.getAvatarUrl().replace("/teacherPhoto/", "");
                        File oldFile = new File(oldPath);
                        if (oldFile.exists()) {
                            oldFile.delete();
                        }
                    }
                }

                // 生成新文件名
                String ext = photo.getOriginalFilename().substring(photo.getOriginalFilename().lastIndexOf("."));
                // 使用 工号作为文件名
                String baseName = teacherPojo.getNum();
                // 过滤非法字符（仅保留字母数字下划线）
                String safeBase = baseName.replaceAll("[^a-zA-Z0-9_\\u4e00-\\u9fa5]", "_"); // 保留中文
                String newFileName = safeBase + ext;

                File dir = new File(uploadFileProperties.getUploadFileTeacherPath());
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                File dest = new File(dir, newFileName);
                photo.transferTo(dest);
                String photoUrl = "/teacherPhoto/" + newFileName;
                teacherPojo.setAvatarUrl(photoUrl);
            } catch (IOException e) {
                return R.error("照片上传失败：" + e.getMessage());
            }
        } else {
            // 若未上传新照片，且是修改操作，保留原 avatarUrl（不设置）
            if (teacherPojo.getId() != null) {
                // 从数据库获取旧对象，避免将 avatarUrl 置空
                TeacherPojo old = teacherService.getById(teacherPojo.getId());
                if (old != null) {
                    teacherPojo.setAvatarUrl(old.getAvatarUrl());
                }
            }
        }

        // 校验编号唯一性
        if (!teacherService.checkNumUnique(teacherPojo.getNum(), teacherPojo.getId())) {
            throw new ResultException(ResultEnum.TEACHER_NUM_EXIST);
        }

        if (teacherPojo.getId() != null) {
            // 修改（不更新密码）
            boolean success = teacherService.saveTeacher(teacherPojo);
            return success ? R.ok("修改成功") : R.error("修改失败");
        } else {
            // 新增
            if (teacherPojo.getPassword() == null || teacherPojo.getPassword().isEmpty()) {
                return R.error("密码不能为空");
            }
            boolean success = teacherService.saveTeacher(teacherPojo);
            return success ? R.ok("添加成功") : R.error("添加失败");

        }
    }

    /**
     * 获取单个教师信息（用于修改回显）
     */
    @GetMapping("/info/{id}")
    @ResponseBody
    @RequiresPermissions("baseinfo:teacher:qurey")
    public R info(@PathVariable Integer id) {
        TeacherPojo teacher = teacherService.getById(id);
        if (teacher == null) {
            return R.error("教师不存在");
        }

        return R.ok().put("data", teacher);
    }
    /**
     * 删除教师
     */
    @DeleteMapping("/delete/{id}")
    @ResponseBody
    @RequiresPermissions("baseinfo:teacher:delete")
    public R delete(@PathVariable Integer id) {
        // 先检查权限
        TeacherPojo teacherPojo = teacherService.getById(id);
        if (teacherPojo == null) return R.error("教师不存在");

        boolean success = teacherService.removeById(id);
        return success ? R.ok("删除成功") : R.error("删除失败");
    }
    /**
     * 重置教师密码
     */
    @PostMapping("/resetPassword")
    @ResponseBody
    @RequiresPermissions("baseinfo:teacher:updatepassword")
    public R resetPassword(@RequestParam Integer teacherId, @RequestParam String newPassword) {
        // 检查权限
        TeacherPojo teacherPojo = teacherService.getById(teacherId);
        if (teacherPojo == null) {
            return R.error("教师不存在");
        }

        if (newPassword == null || newPassword.length() < 6) {
            return R.error("密码长度不能少于6位");
        }
        boolean success = teacherService.resetPassword(teacherId, newPassword);
        return success ? R.ok("密码重置成功") : R.error("密码重置失败");
    }

    @GetMapping("/roleIdsAndChecked/{teacherId}")
    @ResponseBody
    @RequiresPermissions("baseinfo:teacher:zdjs")
    public R getRoleIdsAndChecked(@PathVariable Integer teacherId){
        List<RolePojo> allRoles = roleService.list();
        List<Integer> checkedIds = userRoleService.getRoleIdsByUserIdAndUserType(teacherId, UserTypeConstant.USER_TYPE_TEACHER);
        return  R.ok().put("allRoles",allRoles)
                .put("checkedIds",checkedIds);
    }


    @PostMapping("/saveRoles")
    @ResponseBody
    @RequiresPermissions("baseinfo:teacher:zdjs")
    public R saveRoles(Integer teacherId,Integer roleIds []){
        boolean success = userRoleService.saveRoles(teacherId,roleIds);
        return success?R.ok("指定教师角色成功"):R.error("指定教师角色失败");
    }

}
