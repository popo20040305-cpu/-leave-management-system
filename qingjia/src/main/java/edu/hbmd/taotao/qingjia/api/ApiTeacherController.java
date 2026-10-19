package edu.hbmd.taotao.qingjia.api;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.hbmd.taotao.qingjia.constant.PageConstant;
import edu.hbmd.taotao.qingjia.constant.UserTypeConstant;
import edu.hbmd.taotao.qingjia.controller.BaseController;
import edu.hbmd.taotao.qingjia.exception.ResultEnum;
import edu.hbmd.taotao.qingjia.exception.ResultException;
import edu.hbmd.taotao.qingjia.pojo.ClassPojo;
import edu.hbmd.taotao.qingjia.pojo.CollegePojo;
import edu.hbmd.taotao.qingjia.pojo.RolePojo;
import edu.hbmd.taotao.qingjia.pojo.TeacherPojo;
import edu.hbmd.taotao.qingjia.pojo.UserRolePojo;
import edu.hbmd.taotao.qingjia.properties.UploadFileProperties;
import edu.hbmd.taotao.qingjia.service.ClassService;
import edu.hbmd.taotao.qingjia.service.CollegeService;
import edu.hbmd.taotao.qingjia.service.RoleService;
import edu.hbmd.taotao.qingjia.service.TeacherService;
import edu.hbmd.taotao.qingjia.service.UserRoleService;
import edu.hbmd.taotao.qingjia.util.R;
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
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 教师管理接口（Vue 前端使用），对应菜单「基础信息 -> 教师管理」(/teacher/index)。
 *
 * URL 沿用老的 TeacherController（Thymeleaf 那套），加 /api 前缀、返回 JSON，
 * 分页统一包成 PageResult。老接口保留不动，两套并存。
 *
 * 相比老 controller 的几处实质改动：
 * 1. 新增了分页接口。老 TeacherController 只有 /teacher/query 返回页面（Model），
 *    没有 JSON 版；/teacher/info/{id} 虽然有 @ResponseBody，但它直接把
 *    TeacherPojo 原本返回，里面带着 password（Shiro 加盐后的密文）和 salt。
 *    这里 info 会把这两个字段清空再返回。
 * 2. 分页参数 curPage -> current + size，走 teacherService 新增的重载，
 *    前端选「每页 50 条」才真的拿到 50 行。
 * 3. /save 权限按 id 动态判 add 还是 update。老代码挂的是 add|update 的 OR，
 *    等于「只有新增权限的角色也能改别人的资料」。
 * 4. /save 改成 JSON 请求体（老接口是 multipart 的 teacherJson 字符串 + photo 文件），
 *    头像单独走 /api/teacher/uploadPhoto。这样前端不用为了存一条记录拼 FormData。
 * 5. /delete 加了引用检查：class.banZhuRenId 指向该教师时拒绝删除
 *    （ResultEnum.TEACHER_IS_BANZHUREN 本来就是为这件事准备的，老代码却没用），
 *    删完顺手清掉 user_role 里的关联，避免留下指向不存在教师的角色记录。
 *
 * 仍然继承 BaseController，是为了复用 getLoginUser() 之类的方法。
 * BaseController 里的 collegeService / classService 是包级私有字段、位于 controller 包，
 * 本类在 api 包访问不到，所以下面按 ApiCollegeController 的做法重新声明同名字段。
 */
@RestController
@RequestMapping("/api/teacher")
public class ApiTeacherController extends BaseController {

    private static final String PERM_ACCESS = "baseinfo:teacher:access";
    /** 注意：menu 表里「查询教师」的权限串就是拼错的 qurey，不是 query，必须照抄 */
    private static final String PERM_QUERY  = "baseinfo:teacher:qurey";
    private static final String PERM_ADD    = "baseinfo:teacher:add";
    private static final String PERM_UPDATE = "baseinfo:teacher:update";
    private static final String PERM_DELETE = "baseinfo:teacher:delete";
    private static final String PERM_PWD    = "baseinfo:teacher:updatepassword";
    private static final String PERM_ROLE   = "baseinfo:teacher:zdjs";

    /** 单页条数上限，理由同 ApiCollegeController.MAX_PAGE_SIZE */
    private static final int MAX_PAGE_SIZE = 200;
    /** PageConstant.pageSize 不是 final，注解里不能当编译期常量用 */
    private static final String DEFAULT_PAGE_SIZE = "10";

    @Autowired
    private TeacherService teacherService;
    @Autowired
    private CollegeService collegeService;
    @Autowired
    private ClassService classService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private UserRoleService userRoleService;
    @Autowired
    private UploadFileProperties uploadFileProperties;

    /**
     * 分页条件查询教师。三个条件都可选：
     * num / name 模糊匹配，collegeId 精确匹配。
     * 返回的 TeacherVo 已经 join 出 collegeName，前端不用再拿 id 去字典里翻。
     *
     * SQL 里没有 select password/salt（见 teacherMapper.xml 的 teacher_query_field），
     * 所以列表接口不会泄漏密文。
     */
    @GetMapping("/index")
    @RequiresPermissions(PERM_ACCESS)
    public R index(@RequestParam(value = "current", defaultValue = "1") long current,
                   @RequestParam(value = "size", defaultValue = DEFAULT_PAGE_SIZE) long size,
                   @RequestParam(value = "num", required = false) String num,
                   @RequestParam(value = "name", required = false) String name,
                   @RequestParam(value = "collegeId", required = false) Integer collegeId) {
        int safeCurrent = current < 1 ? 1 : (int) Math.min(current, Integer.MAX_VALUE);
        int safeSize = size < 1 ? PageConstant.pageSize : (int) Math.min(size, MAX_PAGE_SIZE);

        TeacherPojo query = new TeacherPojo();
        query.setNum(trimToNull(num));
        query.setName(trimToNull(name));
        query.setCollegeId(collegeId);

        Page<TeacherVo> page = teacherService.pageTeachers(safeCurrent, safeSize, query);
        return R.ok().put("data", PageResult.of(page));
    }

    /**
     * 教师详情，用于修改弹窗回显。
     * 返回前清空 password / salt —— 修改时前端不需要它们，也不该看到密文。
     */
    @GetMapping("/info/{id}")
    @RequiresPermissions(PERM_QUERY)
    public R info(@PathVariable Integer id) {
        TeacherPojo teacher = teacherService.getById(id);
        if (teacher == null) {
            throw new ResultException(ResultEnum.TEACHER_NOT_EXIST);
        }
        return R.ok().put("data", sanitize(teacher));
    }

    /**
     * 新增 / 修改教师，靠 id 是否为空区分。
     *
     * 新增必须带密码（后端会加盐哈希）；修改时密码留空即可 —— TeacherServiceImpl
     * 的修改分支走 updateById，MyBatis-Plus 默认字段策略是 NOT_NULL，
     * null 字段直接跳过，所以密码、头像不会被清空。
     */
    @PostMapping("/save")
    public R save(@Valid @RequestBody TeacherPojo teacher) {
        if (teacher == null) {
            return R.error(400, "请求体不能为空");
        }
        teacher.setName(trimToNull(teacher.getName()));
        teacher.setNum(trimToNull(teacher.getNum()));
        teacher.setMobile(trimToNull(teacher.getMobile()));

        boolean isUpdate = teacher.getId() != null;
        SecurityUtils.getSubject().checkPermission(isUpdate ? PERM_UPDATE : PERM_ADD);

        if (isUpdate && teacherService.getById(teacher.getId()) == null) {
            throw new ResultException(ResultEnum.TEACHER_NOT_EXIST);
        }
        if (!teacherService.checkNumUnique(teacher.getNum(), teacher.getId())) {
            throw new ResultException(ResultEnum.TEACHER_NUM_EXIST);
        }
        checkCollegeExists(teacher.getCollegeId());

        if (!isUpdate) {
            if (teacher.getPassword() == null || teacher.getPassword().isEmpty()) {
                return R.error(400, "密码不能为空");
            }
            // saveTeacher 内部会生成私盐并加盐哈希，明文密码不会入库
            if (!teacherService.saveTeacher(teacher)) {
                return R.error("添加失败");
            }
            return R.ok("添加成功").put("data", sanitize(teacher));
        }

        // 修改：显式把 password / salt 置空，避免前端误传的字段覆盖已有密码。
        // （即使传了也会被更新进库，所以这里必须清掉，不能只依赖前端不传。）
        teacher.setPassword(null);
        teacher.setSalt(null);
        return teacherService.saveTeacher(teacher) ? R.ok("修改成功") : R.error("修改失败");
    }

    /**
     * 删除教师。
     *
     * 先拦班主任：class.banZhuRenId 还指着他的话必须先解除班主任关系
     * （走班级页的「指定班主任」或改班级），否则删完班级列表里会出现
     * 「班主任姓名空白」的行，而且 user_role 里还留着指向不存在教师的记录。
     *
     * 这里没做 leave_application_shenpi 的引用检查：审批记录属于历史数据，
     * 存量表里也没有外键，删了教师之后审批记录里的 teacherId 会变成悬空 id，
     * 要彻底处理应当改成软删除（数据库加 deleted 标记）而不是物理删除。
     * 这一点先记在这里，本次不改。
     */
    @DeleteMapping("/delete/{id}")
    @RequiresPermissions(PERM_DELETE)
    public R delete(@PathVariable Integer id) {
        if (teacherService.getById(id) == null) {
            throw new ResultException(ResultEnum.TEACHER_NOT_EXIST);
        }

        LambdaQueryWrapper<ClassPojo> classWrapper = new LambdaQueryWrapper<>();
        classWrapper.eq(ClassPojo::getBanZhuRenId, id);
        long classCount = classService.count(classWrapper);
        if (classCount > 0) {
            throw new ResultException(ResultEnum.TEACHER_IS_BANZHUREN.getStatus(),
                    ResultEnum.TEACHER_IS_BANZHUREN.getMessage() + "（当前 " + classCount + " 个班）");
        }

        // 清掉该教师在 user_role 里的记录（指定角色 / 班主任角色）
        LambdaQueryWrapper<UserRolePojo> roleWrapper = new LambdaQueryWrapper<>();
        roleWrapper.eq(UserRolePojo::getUserid, id)
                .eq(UserRolePojo::getUserType, UserTypeConstant.USER_TYPE_TEACHER);
        userRoleService.remove(roleWrapper);

        return teacherService.removeById(id) ? R.ok("删除成功") : R.error("删除失败");
    }

    /**
     * 重置教师密码。后端旧 token 不受影响，但下次登录必须用新密码。
     */
    @PostMapping("/resetPassword")
    @RequiresPermissions(PERM_PWD)
    public R resetPassword(@RequestBody ResetPasswordForm form) {
        if (form == null || form.getTeacherId() == null) {
            return R.error(400, "teacherId 不能为空");
        }
        if (form.getNewPassword() == null || form.getNewPassword().length() < 6) {
            return R.error(400, "密码长度不能少于 6 位");
        }
        if (teacherService.getById(form.getTeacherId()) == null) {
            throw new ResultException(ResultEnum.TEACHER_NOT_EXIST);
        }
        return teacherService.resetPassword(form.getTeacherId(), form.getNewPassword())
                ? R.ok("密码重置成功")
                : R.error("密码重置失败");
    }

    /**
     * 「指定教师角色」弹窗的初始化数据：全部角色 + 该教师当前已勾选的角色 id。
     * 老接口把这两个字段平铺在 R 上（{code,msg,allRoles,checkedIds}），
     * 新接口统一塞进 data，前端拿到的就是 { allRoles, checkedIds }。
     */
    @GetMapping("/{id}/roles")
    @RequiresPermissions(PERM_ROLE)
    public R roleIdsAndChecked(@PathVariable Integer id) {
        if (teacherService.getById(id) == null) {
            throw new ResultException(ResultEnum.TEACHER_NOT_EXIST);
        }
        List<RolePojo> allRoles = roleService.list();
        List<Integer> checkedIds =
                userRoleService.getRoleIdsByUserIdAndUserType(id, UserTypeConstant.USER_TYPE_TEACHER);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("allRoles", allRoles);
        data.put("checkedIds", checkedIds);
        return R.ok().put("data", data);
    }

    /**
     * 保存教师角色。全量覆盖语义（先清空该教师的角色再插入），
     * 传空数组就是「收回全部角色」，见 UserRoleServiceImpl.saveRoles。
     */
    @PostMapping("/{id}/roles")
    @RequiresPermissions(PERM_ROLE)
    public R saveRoles(@PathVariable Integer id, @RequestBody SaveRolesForm form) {
        if (teacherService.getById(id) == null) {
            throw new ResultException(ResultEnum.TEACHER_NOT_EXIST);
        }
        Integer[] roleIds = form == null ? null : form.getRoleIds();
        return userRoleService.saveRoles(id, roleIds)
                ? R.ok("指定教师角色成功")
                : R.error("指定教师角色失败");
    }

    /**
     * 上传教师头像。返回头像相对路径（/teacherPhoto/文件名），前端保存教师时
     * 把它放进 avatarUrl 一起提交。
     *
     * 文件名规则沿用老 TeacherController：用「工号 + 原扩展名」。工号里的非法字符
     * 换成下划线，避免 ../ 之类的路径穿越。
     *
     * 注意：图片文件本身通过 /teacherPhoto/** 静态访问，那条路径在 Shiro 里要放行
     * （application.yml 的 exclude-path-patterns），否则 <img> 请求会被重定向到登录页。
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
        String baseName = (num == null || num.trim().isEmpty())
                ? "teacher_" + System.currentTimeMillis()
                : num.trim().replaceAll("[^a-zA-Z0-9_\\u4e00-\\u9fa5]", "_");

        try {
            File dir = new File(uploadFileProperties.getUploadFileTeacherPath());
            if (!dir.exists() && !dir.mkdirs()) {
                return R.error("创建上传目录失败：" + dir.getAbsolutePath());
            }
            File dest = new File(dir, baseName + ext);
            photo.transferTo(dest);
            return R.ok().put("data", "/teacherPhoto/" + dest.getName());
        } catch (IOException e) {
            return R.error("照片上传失败：" + e.getMessage());
        }
    }

    /**
     * 清掉密码相关字段。列表 SQL 本来就不查这两列，这里主要防 /info 和 /save
     * 把 Shiro 密文和私盐回给前端。
     */
    private TeacherPojo sanitize(TeacherPojo teacher) {
        if (teacher != null) {
            teacher.setPassword(null);
            teacher.setSalt(null);
        }
        return teacher;
    }

    private void checkCollegeExists(Integer collegeId) {
        if (collegeId == null) {
            return;
        }
        CollegePojo college = collegeService.getById(collegeId);
        if (college == null) {
            throw new ResultException(ResultEnum.COLLEGE_NOT_EXIST);
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
        private Integer teacherId;
        private String newPassword;

        public Integer getTeacherId() {
            return teacherId;
        }

        public void setTeacherId(Integer teacherId) {
            this.teacherId = teacherId;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }

    /**
     * 指定角色的请求体。roleIds 允许为 null / 空数组，表示收回全部角色。
     */
    public static class SaveRolesForm {
        private Integer[] roleIds;

        public Integer[] getRoleIds() {
            return roleIds;
        }

        public void setRoleIds(Integer[] roleIds) {
            this.roleIds = roleIds;
        }
    }
}
