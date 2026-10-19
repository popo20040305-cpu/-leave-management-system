package edu.hbmd.taotao.qingjia.api;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.hbmd.taotao.qingjia.constant.PageConstant;
import edu.hbmd.taotao.qingjia.controller.BaseController;
import edu.hbmd.taotao.qingjia.exception.ResultEnum;
import edu.hbmd.taotao.qingjia.exception.ResultException;
import edu.hbmd.taotao.qingjia.pojo.ClassPojo;
import edu.hbmd.taotao.qingjia.pojo.StudentPojo;
import edu.hbmd.taotao.qingjia.pojo.TeacherPojo;
import edu.hbmd.taotao.qingjia.service.ClassService;
import edu.hbmd.taotao.qingjia.service.CollegeService;
import edu.hbmd.taotao.qingjia.service.StudentService;
import edu.hbmd.taotao.qingjia.service.TeacherService;
import edu.hbmd.taotao.qingjia.util.R;
import edu.hbmd.taotao.qingjia.vo.ClassVo;
import edu.hbmd.taotao.qingjia.vo.PageResult;
import edu.hbmd.taotao.qingjia.vo.TeacherVo;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.Logical;
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
import java.util.List;
import java.util.Objects;

/**
 * 班级管理接口（Vue 前端使用），对应菜单「基础信息 -> 班级管理」(/class/index)。
 *
 * 与 ApiCollegeController 同一套路：URL 沿用老 ClassController（Thymeleaf 那套），
 * 加 /api 前缀、返回 JSON、分页统一包成 PageResult。老接口保留不动。
 *
 * 相比老 controller 的几处实质改动：
 * 1. 分页参数 curPage -> current + size，返回 PageResult 而不是 MyBatis-Plus 的 Page
 *    （Page 序列化会带上 orders / optimizeCountSql / searchCount 等前端用不到的内部字段）。
 * 2. /save 权限按 id 动态判 add 还是 update。老代码是 add|update 的 OR —— 只有新增
 *    权限的角色照样能改别人的数据。
 * 3. 指定班主任统一走 classService.setHeadTeacher()，不再直接写 banZhuRenId 字段。
 *    这一点很关键：setHeadTeacher 里除了改 class 表，还会把「班主任」角色从上一个
 *    老师身上摘掉、给新老师挂上（user_role 表）。老 /class/save 直接写字段，
 *    结果是「班级表里写着他是班主任，但他登录后没有班主任角色、看不到审批菜单」。
 * 4. /delete 加了引用检查：class 被 student.classId 引用，库里没有外键约束，
 *    老代码 removeById 一删了之会留下一批指向不存在班级的学生。
 *
 * 数据访问范围（roleType）这里刻意不做过滤：老 ClassController.index 也没做，
 * 班级列表对能看到这个菜单的人就是全量。真要按学院收敛，改 listByCollegeAndGrade
 * 那条路径（BaseController.getClassesByLoginUser 已经写好了规则）。
 */
@RestController
@RequestMapping("/api/class")
public class ApiClassController extends BaseController {

    private static final String PERM_ACCESS    = "baseinfo:class:access";
    private static final String PERM_QUERY     = "baseinfo:class:query";
    private static final String PERM_ADD       = "baseinfo:class:add";
    private static final String PERM_UPDATE    = "baseinfo:class:update";
    private static final String PERM_DELETE    = "baseinfo:class:delete";
    private static final String PERM_BANZHUREN = "baseinfo:class:banzhuren";

    /** 单页条数上限，理由同 ApiCollegeController.MAX_PAGE_SIZE */
    private static final int MAX_PAGE_SIZE = 200;
    /** PageConstant.pageSize 不是 final，注解里不能当编译期常量用 */
    private static final String DEFAULT_PAGE_SIZE = "10";

    @Autowired
    private ClassService classService;
    @Autowired
    private StudentService studentService;
    @Autowired
    private TeacherService teacherService;
    @Autowired
    private CollegeService collegeService;

    /**
     * 分页条件查询班级。四个条件都可选：
     * num 模糊匹配，grade / belongToCollegeId / banZhuRenId 精确匹配。
     * 返回的 ClassVo 里已经 join 出了 collegeName 和 bzrName（班主任姓名），
     * 前端不用再拿 id 去字典里翻。
     */
    @GetMapping("/index")
    @RequiresPermissions(PERM_ACCESS)
    public R index(@RequestParam(value = "current", defaultValue = "1") long current,
                   @RequestParam(value = "size", defaultValue = DEFAULT_PAGE_SIZE) long size,
                   @RequestParam(value = "num", required = false) String num,
                   @RequestParam(value = "grade", required = false) Integer grade,
                   @RequestParam(value = "belongToCollegeId", required = false) Integer belongToCollegeId,
                   @RequestParam(value = "banZhuRenId", required = false) Integer banZhuRenId) {
        int safeCurrent = current < 1 ? 1 : (int) Math.min(current, Integer.MAX_VALUE);
        int safeSize = size < 1 ? PageConstant.pageSize : (int) Math.min(size, MAX_PAGE_SIZE);

        ClassPojo query = new ClassPojo();
        query.setNum(trimToNull(num));
        query.setGrade(grade);
        query.setBelongToCollegeId(belongToCollegeId);
        query.setBanZhuRenId(banZhuRenId);

        Page<ClassVo> page = classService.pageClasses(query, safeCurrent, safeSize);
        return R.ok().put("data", PageResult.of(page));
    }

    /**
     * 不分页的班级列表，给学生管理页的「学院 -> 年级 -> 班级」联动下拉用。
     * 两个条件都可不传，不传就是全量。
     *
     * 权限用的是 class:access：menu 表里「学生」角色是有 baseinfo:class:access 的
     * （学生要能按班级筛自己的同学），所以这里不能挂 query 以外的更高权限。
     */
    @GetMapping("/list")
    @RequiresPermissions(PERM_ACCESS)
    public R list(@RequestParam(value = "collegeId", required = false) Integer collegeId,
                  @RequestParam(value = "grade", required = false) Integer grade) {
        List<ClassPojo> classes = classService.listByCollegeAndGrade(collegeId, grade);
        return R.ok().put("data", classes);
    }

    /**
     * 班级详情，用于修改弹窗回显。
     */
    @GetMapping("/info/{id}")
    @RequiresPermissions(PERM_QUERY)
    public R info(@PathVariable Integer id) {
        ClassPojo clazz = classService.getById(id);
        if (clazz == null) {
            throw new ResultException(ResultEnum.CLASS_NOT_EXIST);
        }
        return R.ok().put("data", clazz);
    }

    /**
     * 新增 / 修改班级，靠 id 是否为空区分。
     *
     * 班主任不在这个方法里直接写字段，见类注释第 3 点。
     */
    @PostMapping("/save")
    public R save(@Valid @RequestBody ClassPojo clazz) {
        if (clazz == null) {
            return R.error(400, "请求体不能为空");
        }
        clazz.setNum(trimToNull(clazz.getNum()));

        boolean isUpdate = clazz.getId() != null;
        SecurityUtils.getSubject().checkPermission(isUpdate ? PERM_UPDATE : PERM_ADD);

        ClassPojo old = null;
        if (isUpdate) {
            old = classService.getById(clazz.getId());
            if (old == null) {
                throw new ResultException(ResultEnum.CLASS_NOT_EXIST);
            }
        }
        checkNumUnique(clazz);
        checkReferences(clazz);

        // 班主任单独拎出来：先把它从待写入的实体上摘掉，避免 save/update 直接写列
        Integer banZhuRenId = clazz.getBanZhuRenId();
        clazz.setBanZhuRenId(isUpdate ? old.getBanZhuRenId() : null);

        if (!isUpdate) {
            if (!classService.save(clazz)) {
                return R.error("新增班级失败");
            }
            if (banZhuRenId != null) {
                classService.setHeadTeacher(clazz.getId(), banZhuRenId);
            }
            return R.ok("新增成功").put("data", clazz);
        }

        // 修改用 UpdateWrapper 显式 set，null 也照写（updateById 的 NOT_NULL 策略会跳过 null，
        // 那样把年级/所属学院清空就存不进去）
        LambdaUpdateWrapper<ClassPojo> update = new LambdaUpdateWrapper<>();
        update.eq(ClassPojo::getId, clazz.getId())
                .set(ClassPojo::getNum, clazz.getNum())
                .set(ClassPojo::getGrade, clazz.getGrade())
                .set(ClassPojo::getBelongToCollegeId, clazz.getBelongToCollegeId());
        if (!classService.update(update)) {
            return R.error("修改班级失败");
        }
        // 只在班主任确实变了的时候才动 user_role，否则每次保存都要白跑一遍角色增删
        if (banZhuRenId != null && !Objects.equals(banZhuRenId, old.getBanZhuRenId())) {
            classService.setHeadTeacher(clazz.getId(), banZhuRenId);
        } else if (banZhuRenId == null && old.getBanZhuRenId() != null) {
            // 清空班主任：只写 class 表，不动 user_role。
            // 那位老师会保留「班主任」角色但名下没有班级，审批列表是空的，无副作用；
            // 反过来若在这里摘角色，而他还是别的班的班主任，就会误伤。
            LambdaUpdateWrapper<ClassPojo> clear = new LambdaUpdateWrapper<>();
            clear.eq(ClassPojo::getId, clazz.getId()).set(ClassPojo::getBanZhuRenId, null);
            classService.update(clear);
        }
        return R.ok("修改成功");
    }

    /**
     * 删除班级。先确认没有学生还挂在这个班上。
     */
    @DeleteMapping("/delete/{id}")
    @RequiresPermissions(PERM_DELETE)
    public R delete(@PathVariable Integer id) {
        if (classService.getById(id) == null) {
            throw new ResultException(ResultEnum.CLASS_NOT_EXIST);
        }
        LambdaQueryWrapper<StudentPojo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StudentPojo::getClassId, id);
        long studentCount = studentService.count(wrapper);
        if (studentCount > 0) {
            throw new ResultException(ResultEnum.CLASS_HAS_STUDENT.getStatus(),
                    ResultEnum.CLASS_HAS_STUDENT.getMessage() + "（当前 " + studentCount + " 名）");
        }
        return classService.removeById(id) ? R.ok("删除成功") : R.error("删除班级失败");
    }

    /**
     * 按姓名模糊搜教师，给「指定班主任」的下拉框用。
     * 走的是 teacherMapper.xml 里的 listBaseInfoTeachers，只查 id/name/collegeId/collegeName，
     * 不含 password/salt。
     *
     * 权限是 add|update|banzhuren 的 OR：新增/修改班级弹窗和单独的「指定班主任」
     * 弹窗都要用它，三者任一即可。老 /class/searchTeachers 完全没挂权限，
     * 等于任何登录用户都能拉到全校教师名单。
     */
    @GetMapping("/teachers")
    @RequiresPermissions(value = {PERM_ADD, PERM_UPDATE, PERM_BANZHUREN}, logical = Logical.OR)
    public R teachers(@RequestParam(value = "name", required = false) String name) {
        List<TeacherVo> teachers = teacherService.searchBaseInfoTeachersByName(trimToNull(name));
        return R.ok().put("data", teachers);
    }

    /**
     * 单独指定班主任（列表页操作列的按钮），和 /save 里那条路径共用 setHeadTeacher，
     * 所以 user_role 里的「班主任」角色也会跟着增删。
     */
    @PostMapping("/setHeadTeacher")
    @RequiresPermissions(PERM_BANZHUREN)
    public R setHeadTeacher(@RequestBody SetHeadTeacherForm form) {
        if (form == null || form.getClassId() == null || form.getTeacherId() == null) {
            return R.error(400, "classId 和 teacherId 都不能为空");
        }
        if (classService.getById(form.getClassId()) == null) {
            throw new ResultException(ResultEnum.CLASS_NOT_EXIST);
        }
        if (teacherService.getById(form.getTeacherId()) == null) {
            throw new ResultException(ResultEnum.TEACHER_NOT_EXIST);
        }
        classService.setHeadTeacher(form.getClassId(), form.getTeacherId());
        return R.ok("指定班主任成功");
    }

    /**
     * 班级编号库内唯一，修改时排除自己这一行。规则与老 ClassController.save 一致。
     */
    private void checkNumUnique(ClassPojo clazz) {
        if (clazz.getNum() == null) {
            return;
        }
        LambdaQueryWrapper<ClassPojo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClassPojo::getNum, clazz.getNum());
        if (clazz.getId() != null) {
            wrapper.ne(ClassPojo::getId, clazz.getId());
        }
        if (classService.count(wrapper) > 0) {
            throw new ResultException(ResultEnum.ClASS_ADD_UPDATE_SAME_NAME);
        }
    }

    /**
     * 外键完整性靠代码兜：class 表引用了 college 和 teacher，库里都没有外键约束，
     * 写进去一个不存在的 id 不会报错，但列表 join 出来的学院名/班主任名会是空，
     * 而且 setHeadTeacher 会往 user_role 里塞一条指向不存在教师的记录。
     */
    private void checkReferences(ClassPojo clazz) {
        if (clazz.getBelongToCollegeId() != null
                && collegeService.getById(clazz.getBelongToCollegeId()) == null) {
            throw new ResultException(ResultEnum.COLLEGE_NOT_EXIST);
        }
        if (clazz.getBanZhuRenId() != null) {
            TeacherPojo teacher = teacherService.getById(clazz.getBanZhuRenId());
            if (teacher == null) {
                throw new ResultException(ResultEnum.TEACHER_NOT_EXIST);
            }
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 指定班主任的请求体。用显式字段而不是 Map，
     * 免得 @RequestParam 那种「参数名写错也不报错、静默变成 null」的坑。
     */
    public static class SetHeadTeacherForm {
        private Integer classId;
        private Integer teacherId;

        public Integer getClassId() {
            return classId;
        }

        public void setClassId(Integer classId) {
            this.classId = classId;
        }

        public Integer getTeacherId() {
            return teacherId;
        }

        public void setTeacherId(Integer teacherId) {
            this.teacherId = teacherId;
        }
    }
}
