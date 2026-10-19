package edu.hbmd.taotao.qingjia.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.hbmd.taotao.qingjia.exception.ResultEnum;
import edu.hbmd.taotao.qingjia.exception.ResultException;
import edu.hbmd.taotao.qingjia.pojo.CollegePojo;
import edu.hbmd.taotao.qingjia.service.CollegeService;
import edu.hbmd.taotao.qingjia.util.R;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

import static edu.hbmd.taotao.qingjia.constant.PageConstant.pageSize;

@Controller
@RequestMapping("/college")
public class CollegeController extends BaseController{
    @Autowired
    CollegeService collegeService;

    @GetMapping("/list")
    @RequiresPermissions("baseinfo.college.access")
    public String list(){
        return "/college/list";
    }

    /**
     * 学院管理主页（分页 + 条件查询）
     * @param curPage 当前页码，默认1
     * @param college 查询条件（封装name, num等）
     * @param model 模型
     * @return 视图路径
     */
    @GetMapping("/index")
    @RequiresPermissions("baseinfo:college:access")
    public String index(@RequestParam(defaultValue = "1") Integer curPage,
                        CollegePojo college,
                        Model model) {
        // 调用 service 分页查询
        Page<CollegePojo> resultPage = collegeService.queryPage(curPage, pageSize, college);
        // 回显查询条件
        model.addAttribute("page", resultPage);
        //model.addAttribute("collegeQuery", college);
        return "/college/list";
    }

    /**
     * 保存或更新学院信息（JSR303 自动校验）
     * @param college 学院实体，添加 @Valid 触发校验
     * @return R
     */
    @PostMapping("/save")
    @ResponseBody
    @RequiresPermissions("baseinfo:college:add")
    public R save(@Valid @RequestBody CollegePojo college) {
        LambdaQueryWrapper<CollegePojo> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(CollegePojo::getName, college.getName())
                .or()
                .eq(CollegePojo::getNum, college.getNum()));
        if (college.getId() != null) {
            wrapper.ne(CollegePojo::getId, college.getId());
        }
        if (collegeService.count(wrapper)>0){
            throw new ResultException(ResultEnum.COLLEGE_ADD_UPDATE_SAME_NAME);
        }
        // 校验通过后执行保存/更新
        boolean success = collegeService.saveOrUpdate(college);
        if (success) {
            return R.ok("保存成功");
        }
        return R.error("保存失败");
    }

    /**
     * 根据ID获取学院信息（修改回显）
     */
    @GetMapping("/info/{id}")
    @ResponseBody
    @RequiresPermissions("baseinfo:college:query")
    public R info(@PathVariable Integer id) {
        CollegePojo college = collegeService.getById(id);
        if (college != null) {
            return R.ok().put("data", college);
        }
        return R.error("学院不存在");
    }
    /**
     * 删除学院信息
     */
    @DeleteMapping("/delete/{id}")
    @ResponseBody
    @RequiresPermissions("baseinfo:college:delete")
    public R delete(@PathVariable Integer id) {
        boolean success = collegeService.removeById(id);
        if (success) {
            return R.ok("删除成功");
        }
        return R.error("删除失败");
    }



}
