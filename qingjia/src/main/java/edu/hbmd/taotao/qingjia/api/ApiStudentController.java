package edu.hbmd.taotao.qingjia.api;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.hbmd.taotao.qingjia.constant.PageConstant;
import edu.hbmd.taotao.qingjia.constant.RoleTypeConstant;
import edu.hbmd.taotao.qingjia.controller.BaseController;
import edu.hbmd.taotao.qingjia.exception.ResultEnum;
import edu.hbmd.taotao.qingjia.exception.ResultException;
import edu.hbmd.taotao.qingjia.pojo.ClassPojo;
import edu.hbmd.taotao.qingjia.pojo.StudentPojo;
import edu.hbmd.taotao.qingjia.properties.UploadFileProperties;
import edu.hbmd.taotao.qingjia.service.ClassService;
import edu.hbmd.taotao.qingjia.service.StudentService;
import edu.hbmd.taotao.qingjia.shiro.LoginUser;
import edu.hbmd.taotao.qingjia.util.R;
import edu.hbmd.taotao.qingjia.vo.PageResult;
import edu.hbmd.taotao.qingjia.vo.StudentVo;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.File;
import java.io.IOException;

/**
 * 学生管理接口（Vue 前端使用），对应菜单「基础信息 -> 学生管理」(/student/index)。
 *
 * URL 沿用老的 StudentController（Thymeleaf 那套），加 /api 前缀、返回 JSON，
 * 分页统一包成 PageResult。老接口保留不动。
 *
 * 相比老 controller 的几处实质改动：
 * 1. 分页参数 curPage -> current + size（走 StudentService 新增的重载），
 *    前端选「每页 50 条」时才真的拿到 50 行。
 * 2. /save 权限按 id 动态判 add 还是 update。老代码挂的是 add|update 的 OR，
 *    只有新增权限的角色照样能改别人的资料。
 * 3. /save 改成 JSON 请求体（老接口是 multipart 的 studentJson 字符串 + photo 文件），
 *    头像单独走 /api/student/uploadPhoto。
 * 4. 手机号 / QQ 留空时统一存 null，不再把空串写进库；
 *    StudentPojo 上那两条 @Pattern 也已放开空串（见那里的注释）。
 * 5. 数据范围：roleType 为「个人」的学生只能查到自己那一条，
 *    这是老 query 里就有的规则（loginUser 是学生且 roleType=个人时强制 id=自己），
 *    这里原样保留 —— 但老版本只在 /student/query 里做了，/student/info/{id}
 *    谁都能拿别人的 id 来查，新接口两个入口都做了限制。
 *
 * 没有删除接口：menu 表里「学生管理」只有 查询/添加/修改/详情/修改密码 五条权限，
 * 本来就没有删除权限，老 StudentController 也没写删除。要支持删学生得先在菜单表加权限。
 *
 * 和 ApiCollegeController / ApiClassController 一样继承 BaseController 复用
 * getLoginUser()；BaseController 里的 classService 在 controller 包、包级私有，
 * 这里访问不到，所以重新声明一个同名字段。
 */
@RestController
@RequestMapping("/api/student")
public class ApiStudentController extends BaseController {

    private static final String PERM_ACCESS = "baseinfo:student:access";
    private static final String PERM_QUERY  = "baseinfo:student:query";
    private static final String PERM_ADD    = "baseinfo:student:add";
    private static final String PERM_UPDATE = "baseinfo:student:update";
    private static final String PERM_PWD    = "baseinfo:student:updatepassword";

    /** 单页条数上限，理由同 ApiCollegeController.MAX_PAGE_SIZE */
    private static final int MAX_PAGE_SIZE = 200;
    /** PageConstant.pageSize 不是 final，注解里不能当编译期常量用 */
    private static final String DEFAULT_PAGE_SIZE = "10";

    @Autowired
    private StudentService studentService;
    @Autowired
    private ClassService classService;
    @Autowired
    private UploadFileProperties uploadFileProperties;

    /**
     * 分页条件查询学生。条件都可选：
     * num / name 模糊匹配；collegeId / grade / classId 精确匹配
     * （年级和学院实际过滤的是 join 出来的 class.grade / college.id）。
     * 返回的 StudentVo 已经 join 出 className / grade / collegeId / collegeName，
     * 前端表格不用再拿 id 去字典里翻。
     *
     * SQL 里没有 select password/salt（见 studentMapper.xml 的 student_query_field），
     * 所以列表和详情都不会泄漏密文。
     */
    @GetMapping("/index")
    @RequiresPermissions(PERM_ACCESS)
    public R index(@RequestParam(value = "current", defaultValue = "1") long current,
                   @RequestParam(value = "size", defaultValue = DEFAULT_PAGE_SIZE) long size,
                   @RequestParam(value = "num", required = false) String num,
                   @RequestParam(value = "name", required = false) String name,
                   @RequestParam(value = "collegeId", required = false) Integer collegeId,
                   @RequestParam(value = "grade", required = false) Integer grade,
                   @RequestParam(value = "classId", required = false) Integer classId) {
        int safeCurrent = current < 1 ? 1 : (int) Math.min(current, Integer.MAX_VALUE);
        int safeSize = size < 1 ? PageConstant.pageSize : (int) Math.min(size, MAX_PAGE_SIZE);

        StudentVo query = new StudentVo();
        query.setNum(trimToNull(num));
        query.setName(trimToNull(name));
        query.setCollegeId(collegeId);
        query.setGrade(grade);
        query.setClassId(classId);

        // 「个人」数据范围的学生只能看自己：老 StudentController.query 就是这条规则，
        // 少了它会让学生通过改 URL 参数把全校同学都翻出来。
        Integer selfId = getSelfStudentIdIfPersonScope();
        if (selfId != null) {
            query.setId(selfId);
        }

        Page<StudentVo> page = studentService.pageStudents(safeCurrent, safeSize, query);
        // 列表 SQL 不查 password/salt，无需再清洗
        return R.ok().put("data", PageResult.of(page));
    }

    /**
     * 学生详情（修改弹窗回显 / 学生详情弹窗）。
     * 返回值由 studentMapper.selectById 拼好班级名、年级、学院名，
     * 并且不包含 password / salt。
     */
    @GetMapping("/info/{id}")
    @RequiresPermissions(PERM_QUERY)
    public R info(@PathVariable Integer id) {
        ensureSelfScopeAllowed(id);
        StudentVo student = studentService.selectById(id);
        if (student == null) {
            throw new ResultException(ResultEnum.STUDENT_NOT_EXIST);
        }
        return R.ok().put("data", student);
    }

    /**
     * 新增 / 修改学生，靠 id 是否为空区分。
     *
     * 新增必须带密码（后端加盐哈希后入库）；修改时密码留空即可 ——
     * StudentServiceImpl 的修改分支走 updateById，MyBatis-Plus 默认字段策略
     * 是 NOT_NULL，null 字段跳过，所以密码和头像不会被清空。
     */
    @PostMapping("/save")
    public R save(@Valid @RequestBody StudentPojo student) {
        if (student == null) {
            return R.error(400, "请求体不能为空");
        }
        student.setName(trimToNull(student.getName()));
        student.setNum(trimToNull(student.getNum()));
        // 空串一律归一成 null，避免库里同时存在 '' 和 null 两种「没填」
        student.setMobile(trimToNull(student.getMobile()));
        student.setQq(trimToNull(student.getQq()));

        boolean isUpdate = student.getId() != null;
        SecurityUtils.getSubject().checkPermission(isUpdate ? PERM_UPDATE : PERM_ADD);

        if (isUpdate) {
            if (studentService.getById(student.getId()) == null) {
                throw new ResultException(ResultEnum.STUDENT_NOT_EXIST);
            }
            ensureSelfScopeAllowed(student.getId());
        }
        if (!studentService.checkNumUnique(student)) {
            throw new ResultException(ResultEnum.STUDENT_NUM_EXIST);
        }
        checkClassExists(student.getClassId());

        if (!isUpdate) {
            if (student.getPassword() == null || student.getPassword().isEmpty()) {
                return R.error(400, "密码不能为空");
            }
            // saveStudent 内部生成私盐并加盐哈希，明文不会入库
            if (!studentService.saveStudent(student)) {
                return R.error("添加失败");
            }
            return R.ok("添加成功").put("data", student);
        }

        // 修改：显式清掉密码相关字段，防止前端误传覆盖已有密码
        student.setPassword(null);
        student.setSalt(null);
        return studentService.saveStudent(student) ? R.ok("修改成功") : R.error("修改失败");
    }

    /**
     * 重置学生密码。
     */
    @PostMapping("/resetPassword")
    @RequiresPermissions(PERM_PWD)
    public R resetPassword(@RequestBody ResetPasswordForm form) {
        if (form == null || form.getStudentId() == null) {
            return R.error(400, "studentId 不能为空");
        }
        if (form.getNewPassword() == null || form.getNewPassword().length() < 6) {
            return R.error(400, "密码长度不能少于 6 位");
        }
        if (studentService.getById(form.getStudentId()) == null) {
            throw new ResultException(ResultEnum.STUDENT_NOT_EXIST);
        }
        return studentService.resetPassword(form.getStudentId(), form.getNewPassword())
                ? R.ok("密码重置成功")
                : R.error("密码重置失败");
    }

    /**
     * 上传学生头像，返回相对路径（/studentPhoto/文件名），
     * 前端保存学生时放进 avatarUrl 提交。
     *
     * 文件名规则沿用老 StudentController：学号 + 时间戳 + 原扩展名。
     * 图片本身通过 /studentPhoto/** 访问，那条路径在 Shiro 里要放行，
     * 否则 <img> 会被重定向到登录页（见 application.yml 的 exclude-path-patterns）。
     */
    @PostMapping("/uploadPhoto")
    @RequiresPermissions(value = {PERM_ADD, PERM_UPDATE}, logical = Logical.OR)
    public R uploadPhoto(@RequestParam("photo") MultipartFile photo,
                         @RequestParam(value = "num", required = false) String num) {
        if (photo == null || photo.isEmpty()) {
            return R.error(400, "请选择要上传的照片");
        }
        String originalName = photo.getOriginalFilename();
        if (originalName == null || !originalName.contains(".")) {
            return R.error(400, "文件名不合法");
        }
        String ext = originalName.substring(originalName.lastIndexOf("."));
        String prefix = (num == null || num.trim().isEmpty())
                ? "student"
                : num.trim().replaceAll("[^a-zA-Z0-9_\\u4e00-\\u9fa5]", "_");

        try {
            File dir = new File(uploadFileProperties.getUploadFileStudentPath());
            if (!dir.exists() && !dir.mkdirs()) {
                return R.error("创建上传目录失败：" + dir.getAbsolutePath());
            }
            File dest = new File(dir, prefix + "_" + System.currentTimeMillis() + ext);
            photo.transferTo(dest);
            return R.ok().put("data", "/studentPhoto/" + dest.getName());
        } catch (IOException e) {
            return R.error("照片上传失败：" + e.getMessage());
        }
    }

    /**
     * 「个人」数据范围的学生（roleType=1 且 type=学生）只能操作自己，
     * 返回自己的学生 id；其他情况返回 null（表示不做限制）。
     *
     * 注意：班主任 / 辅导员这些 roleType 为班级、学院的教师不走这条分支。
     */
    private Integer getSelfStudentIdIfPersonScope() {
        LoginUser loginUser = getLoginUser();
        if (loginUser == null || loginUser.getStudent() == null) {
            return null;
        }
        if (loginUser.getType() != LoginUser.TYPE_STUDENT) {
            return null;
        }
        if (loginUser.getRoleType() != RoleTypeConstant.ROLE_TYPE_PERSON) {
            return null;
        }
        return loginUser.getStudent().getId();
    }

    /**
     * 同上的数据范围校验，用在拿到具体 id 的接口上（详情 / 修改）：
     * 个人范围的学生访问别人的 id 直接拒绝，避免「列表只能看自己、
     * 但把 URL 里的 id 一改就能看别人」。
     */
    private void ensureSelfScopeAllowed(Integer targetId) {
        Integer selfId = getSelfStudentIdIfPersonScope();
        if (selfId != null && !selfId.equals(targetId)) {
            throw new ResultException(ResultEnum.LEAVE_APPLIICATION_NO_PERMISSION.getStatus(),
                    "只能查看本人的信息");
        }
    }

    private void checkClassExists(Integer classId) {
        if (classId == null) {
            return;
        }
        ClassPojo clazz = classService.getById(classId);
        if (clazz == null) {
            throw new ResultException(ResultEnum.CLASS_NOT_EXIST);
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
     * 重置密码的请求体。显式字段而不是 Map，免得参数名写错也不报错。
     */
    public static class ResetPasswordForm {
        private Integer studentId;
        private String newPassword;

        public Integer getStudentId() {
            return studentId;
        }

        public void setStudentId(Integer studentId) {
            this.studentId = studentId;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }
}
