package edu.hbmd.taotao.qingjia.api;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.hbmd.taotao.qingjia.constant.PageConstant;
import edu.hbmd.taotao.qingjia.controller.BaseController;
import edu.hbmd.taotao.qingjia.exception.ResultEnum;
import edu.hbmd.taotao.qingjia.exception.ResultException;
import edu.hbmd.taotao.qingjia.pojo.ClassPojo;
import edu.hbmd.taotao.qingjia.pojo.CollegePojo;
import edu.hbmd.taotao.qingjia.pojo.TeacherPojo;
import edu.hbmd.taotao.qingjia.service.ClassService;
import edu.hbmd.taotao.qingjia.service.CollegeService;
import edu.hbmd.taotao.qingjia.service.TeacherService;
import edu.hbmd.taotao.qingjia.util.R;
import edu.hbmd.taotao.qingjia.vo.PageResult;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 学院管理接口（Vue 前端使用），对应菜单「基础信息 -> 学院管理」(/college/index)。
 *
 * URL 与老的 CollegeController（Thymeleaf 那套）一一对应，只是加了 /api 前缀、
 * 改成返回 JSON，两套并存，老页面不受影响。
 *
 * 相对这个文件最初的版本改了 5 处，都是为了配合前端 src/api/collegeApi.ts：
 *
 * 1. @Controller -> @RestController。原来方法直接 return R，但类上是 @Controller
 *    又没有 @ResponseBody，Spring 会把 R 当成 Model 属性、再拿请求路径去找视图名，
 *    结果是模板解析失败的 500 而不是 JSON。同目录的 ApiMenuController /
 *    ApiBaseInfoController / ApiBaseDataController 用的都是 @RestController。
 *
 * 2. /index 的返回体从 { page: MyBatis-Plus 的 Page, pageUrl: ... } 换成 PageResult。
 *    pageUrl 是 PageUrlFilter 给 Thymeleaf 分页片段 templates/common/pageCommon.html
 *    拼 a 标签 href 用的，SPA 里分页条由前端 Pagination 组件自己算页码，这个字段恒为 null；
 *    Page 直接序列化则会带上 orders / optimizeCountSql / searchCount 等前端用不到的
 *    内部字段，而且字段会随 MyBatis-Plus 版本变化。
 *    PageResult 的 5 个字段正好对上前端 src/type/page.ts 的 PageResult。
 *
 * 3. 分页参数 curPage -> current + size，与前端 usePageTable 发出去的字段名对齐；
 *    原来走 BaseController.getPageSize()，只认 pageSize / limit 两个名字，
 *    前端选「每页 50 条」后端收不到，永远只返回 10 条。
 *
 * 4. /save 的权限按 id 是否存在动态判定（见方法内注释）。原来只挂了
 *    baseinfo:college:add，等于「有新增权限的人也能改数据」。
 *    另外修改分支不再用 saveOrUpdate：MyBatis-Plus 默认字段策略是 NOT_NULL，
 *    null 字段会被跳过，用户把「办公电话」清空后提交，库里旧号码还在。
 *
 * 5. /delete 加了引用检查。college 被 class.belong_to_college_id 和 teacher.college_id
 *    直接引用，库里又没有外键约束，原来 removeById 一删了之会留下指向不存在学院的孤儿数据。
 *
 * 仍然继承 BaseController：后面若要按登录用户过滤学院列表，
 * getCollegesByLoginUserFromDb() 就在那里。注意 BaseController 里的 collegeService /
 * classService 是包级私有的、且位于 controller 包，本类在 api 包访问不到，
 * 所以下面重新声明了同名字段（字段隐藏，两边都会被 Spring 注入，互不影响）。
 */
@RestController
@RequestMapping("/api/college")
public class ApiCollegeController extends BaseController {

    private static final String PERM_ACCESS = "baseinfo:college:access";
    private static final String PERM_QUERY  = "baseinfo:college:query";
    private static final String PERM_ADD    = "baseinfo:college:add";
    private static final String PERM_UPDATE = "baseinfo:college:update";
    private static final String PERM_DELETE = "baseinfo:college:delete";

    /**
     * 单页条数上限。前端 el-pagination 最大只到 100，这里放宽到 200 留余量，
     * 但不允许无上限 —— 否则 size=999999 等于把整张表捞进内存再序列化。
     */
    private static final int MAX_PAGE_SIZE = 200;

    /**
     * 默认每页条数。
     * 不能直接写 @RequestParam(defaultValue = "" + PageConstant.pageSize) ——
     * PageConstant.pageSize 是 static 但不是 final，不是编译期常量，注解里用会编译不过。
     */
    private static final String DEFAULT_PAGE_SIZE = "10";

    @Autowired
    private CollegeService collegeService;
    @Autowired
    private ClassService classService;
    @Autowired
    private TeacherService teacherService;

    /**
     * 分页条件查询。name / num 都是模糊匹配，与老 /college/index 一致。
     *
     * @param current 页码，从 1 开始
     * @param size    每页条数
     * @param name    学院名称，可空
     * @param num     学院编号，可空
     */
    @GetMapping("/index")
    @RequiresPermissions(PERM_ACCESS)
    public R index(@RequestParam(value = "current", defaultValue = "1") long current,
                   @RequestParam(value = "size", defaultValue = DEFAULT_PAGE_SIZE) long size,
                   @RequestParam(value = "name", required = false) String name,
                   @RequestParam(value = "num", required = false) String num) {
        int safeCurrent = current < 1 ? 1 : (int) Math.min(current, Integer.MAX_VALUE);
        int safeSize = size < 1 ? PageConstant.pageSize : (int) Math.min(size, MAX_PAGE_SIZE);

        // 查询条件用 CollegePojo 承载，复用 service 里已有的条件构造（和老 controller 一样）
        CollegePojo query = new CollegePojo();
        query.setName(trimToNull(name));
        query.setNum(trimToNull(num));

        Page<CollegePojo> page = collegeService.queryPage(safeCurrent, safeSize, query);
        return R.ok().put("data", PageResult.of(page));
    }

    /**
     * 根据 id 获取学院详情，用于修改弹窗回显。
     */
    @GetMapping("/info/{id}")
    @RequiresPermissions(PERM_QUERY)
    public R info(@PathVariable Integer id) {
        CollegePojo college = collegeService.getById(id);
        if (college == null) {
            throw new ResultException(ResultEnum.COLLEGE_NOT_EXIST);
        }
        return R.ok().put("data", college);
    }

    /**
     * 新增 / 修改学院（沿用老接口的 saveOrUpdate 语义，靠 id 是否为空区分）。
     *
     * 字段校验交给 CollegePojo 上的 JSR303 注解（name/num 非空、tel 3-15 位数字），
     * 校验失败由全局 ResultExceptionHandler 统一转成 JSON。
     */
    @PostMapping("/save")
    public R save(@Valid @RequestBody CollegePojo college) {
        if (college == null) {
            return R.error(400, "请求体不能为空");
        }
        normalize(college);

        // 权限不能用 @RequiresPermissions 静态挂：同一个方法既要管新增又要管修改，
        // 而这两件事在 menu 表里是两个不同的权限串。老代码只挂了 add，
        // 结果只有新增权限、没有修改权限的角色照样能改数据。
        // 这里按 id 动态校验，checkPermission 不通过会抛 UnauthorizedException，
        // 由 ResultExceptionHandler 转成 code=403 的 JSON。
        boolean isUpdate = college.getId() != null;
        SecurityUtils.getSubject().checkPermission(isUpdate ? PERM_UPDATE : PERM_ADD);

        if (isUpdate && collegeService.getById(college.getId()) == null) {
            throw new ResultException(ResultEnum.COLLEGE_NOT_EXIST);
        }
        checkDuplicate(college);

        if (!isUpdate) {
            // save 会把自增 id 回填到 college 上，直接返回给前端
            return collegeService.save(college)
                    ? R.ok("新增成功").put("data", college)
                    : R.error("新增学院失败");
        }

        // 修改不能用 saveOrUpdate/updateById：默认字段策略 NOT_NULL 会跳过 null 字段，
        // 那样用户把「办公电话」清空后提交，库里旧号码还在，界面刷新一下又冒出来，
        // 看起来像没保存成功。所以这里用 UpdateWrapper 显式 set 三个字段，null 也照写。
        LambdaUpdateWrapper<CollegePojo> update = new LambdaUpdateWrapper<>();
        update.eq(CollegePojo::getId, college.getId())
                .set(CollegePojo::getName, college.getName())
                .set(CollegePojo::getNum, college.getNum())
                .set(CollegePojo::getTel, college.getTel());
        return collegeService.update(update) ? R.ok("修改成功") : R.error("修改学院失败");
    }

    /**
     * 删除学院。先检查有没有班级 / 教师挂在这个学院下。
     */
    @DeleteMapping("/delete/{id}")
    @RequiresPermissions(PERM_DELETE)
    public R delete(@PathVariable Integer id) {
        if (collegeService.getById(id) == null) {
            throw new ResultException(ResultEnum.COLLEGE_NOT_EXIST);
        }

        LambdaQueryWrapper<ClassPojo> classWrapper = new LambdaQueryWrapper<>();
        classWrapper.eq(ClassPojo::getBelongToCollegeId, id);
        long classCount = classService.count(classWrapper);
        if (classCount > 0) {
            throw new ResultException(ResultEnum.COLLEGE_HAS_CLASS.getStatus(),
                    ResultEnum.COLLEGE_HAS_CLASS.getMessage() + "（当前 " + classCount + " 个）");
        }

        LambdaQueryWrapper<TeacherPojo> teacherWrapper = new LambdaQueryWrapper<>();
        teacherWrapper.eq(TeacherPojo::getCollegeId, id);
        long teacherCount = teacherService.count(teacherWrapper);
        if (teacherCount > 0) {
            throw new ResultException(ResultEnum.COLLEGE_HAS_TEACHER.getStatus(),
                    ResultEnum.COLLEGE_HAS_TEACHER.getMessage() + "（当前 " + teacherCount + " 名）");
        }

        return collegeService.removeById(id) ? R.ok("删除成功") : R.error("删除学院失败");
    }

    /**
     * 名称或编号重复校验，规则与老 CollegeController.save 一致：
     * name 或 num 任一撞上就算重复；修改时排除自己这一行。
     */
    private void checkDuplicate(CollegePojo college) {
        LambdaQueryWrapper<CollegePojo> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(CollegePojo::getName, college.getName())
                .or()
                .eq(CollegePojo::getNum, college.getNum()));
        if (college.getId() != null) {
            wrapper.ne(CollegePojo::getId, college.getId());
        }
        if (collegeService.count(wrapper) > 0) {
            throw new ResultException(ResultEnum.COLLEGE_ADD_UPDATE_SAME_NAME);
        }
    }

    /**
     * 去掉首尾空格；办公电话留空时存 null 而不是空串，
     * 避免库里同时存在 '' 和 null 两种「没填」。
     */
    private void normalize(CollegePojo college) {
        college.setName(trimToNull(college.getName()));
        college.setNum(trimToNull(college.getNum()));
        college.setTel(trimToNull(college.getTel()));
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
