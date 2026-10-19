package edu.hbmd.taotao.qingjia.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.hbmd.taotao.qingjia.exception.ResultEnum;
import edu.hbmd.taotao.qingjia.exception.ResultException;
import edu.hbmd.taotao.qingjia.pojo.ClassPojo;
import edu.hbmd.taotao.qingjia.service.ClassService;
import edu.hbmd.taotao.qingjia.service.TeacherService;
import edu.hbmd.taotao.qingjia.util.R;
import edu.hbmd.taotao.qingjia.vo.ClassVo;
import edu.hbmd.taotao.qingjia.vo.TeacherVo;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

@Controller
@RequestMapping("/class")
public class ClassController extends BaseController{
    @Autowired
    ClassService classService;

    @Autowired
    TeacherService teacherService;

    /**
     * 班级管理页面
     */
    @GetMapping("/index")
    @RequiresPermissions("baseinfo:class:query")
    public String index(@RequestParam(defaultValue = "1") Integer curPage,
            Model model, ClassPojo clazz, HttpServletRequest request) {
        // 分页查询
        Page<ClassVo> page = classService.pageClasses(clazz,curPage);

        model.addAttribute("page", page);
        model.addAttribute("classQuery", clazz);
        model.addAttribute(BaseController.COLLEGES, getColleges());
        model.addAttribute(BaseController.GRADES, getGrades());
        return "class/list";
    }
    /**
     * 保存班级（添加或修改）
     */
    @PostMapping("/save")
    @ResponseBody
    @RequiresPermissions(value = {"baseinfo:class:add","baseinfo:class:update"},logical = Logical.OR)
    public R save(@Valid @RequestBody ClassPojo classPojo) {
        LambdaQueryWrapper<ClassPojo> wrapper = new LambdaQueryWrapper();
        wrapper.eq(ClassPojo::getNum,classPojo.getNum());
        if (classPojo.getId()!=null){
            wrapper.ne(ClassPojo::getId,classPojo.getId());
        }
        if (classService.count(wrapper)>0){
            throw new ResultException(ResultEnum.ClASS_ADD_UPDATE_SAME_NAME);
        }
        if (classPojo.getId()!=null){
            if (classService.updateById(classPojo)){
                return R.ok("班级信息修改成功");
            }else{
                return R.ok("班级信息修改失败");
            }
        }else{
            if (classService.save(classPojo)){
                return R.ok("班级信息保存成功");
            }else{
                return R.ok("班级信息保存失败");
            }
        }
    }

    /**
     * 获取班级信息（用于修改）
     */
    @GetMapping("/info/{id}")
    @ResponseBody
    @RequiresPermissions("baseinfo:class:query")
    public R info(@PathVariable Integer id) {
        ClassPojo classPojo = classService.getById(id);
        if (classPojo != null) {
            return  R.ok().put("data", classPojo);
        } else {
            return R.error("班级不存在");
        }
    }
    /**
     * 删除班级
     */
    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public R delete(@PathVariable Integer id) {
        try {
            classService.removeById(id);
            return  R.ok("删除成功");
        } catch (Exception e) {
            return R.error("删除失败：" + e.getMessage());
        }
    }

    /**
     * 搜索教师（用于指定班主任）
     */
    @GetMapping("/searchTeachers")
    @ResponseBody
    public List<TeacherVo> searchTeachers(@RequestParam(required = false) String name) {
        return teacherService.searchBaseInfoTeachersByName(name);
    }
    /**
     * 指定班主任
     */
    @PostMapping("/setHeadTeacher")
    @ResponseBody
    public R setHeadTeacher(@RequestParam Integer classId, @RequestParam Integer teacherId) {
        try {
            ClassPojo classPojo = classService.getById(classId);
            if (classPojo != null) {
                classService.setHeadTeacher(classId,teacherId);
                return R.ok("指定班主任成功");
            } else {
                return R.error("班级不存在");
            }
        } catch (Exception e) {
            return R.error("操作失败：" + e.getMessage());
        }
    }

}
