package edu.hbmd.taotao.qingjia.api;

import cn.hutool.core.util.IdUtil;
import edu.hbmd.taotao.qingjia.constant.UserTypeConstant;
import edu.hbmd.taotao.qingjia.controller.BaseController;
import edu.hbmd.taotao.qingjia.exception.ResultEnum;
import edu.hbmd.taotao.qingjia.exception.ResultException;
import edu.hbmd.taotao.qingjia.pojo.StudentPojo;
import edu.hbmd.taotao.qingjia.pojo.TeacherPojo;
import edu.hbmd.taotao.qingjia.pojo.UserTokenPojo;
import edu.hbmd.taotao.qingjia.service.StudentService;
import edu.hbmd.taotao.qingjia.service.TeacherService;
import edu.hbmd.taotao.qingjia.service.UserTokenService;
import edu.hbmd.taotao.qingjia.shiro.LoginUser;
import edu.hbmd.taotao.qingjia.shiro.QingjiaUsernamePasswordToken;
import edu.hbmd.taotao.qingjia.shiro.ShiroProperties;
import edu.hbmd.taotao.qingjia.util.PasswordUtil;
import edu.hbmd.taotao.qingjia.util.R;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 前后端分离登录接口（Vue 前端使用）。
 *
 * 修复点：
 * 1. 改成 POST + JSON body。原来是 GET + query 参数，密码会进访问日志，
 *    而且和前端 axios 的 post('/api/login', {...}) 对不上。
 * 2. /api/login、/api/logout 必须在 application.yml 的 shiro 放行列表里，
 *    否则会被 bearerFilter 拦住，出现"拿 token 的接口自己也要 token"的死循环。
 * 3. salt 为空时给出明确业务错误，不再 NPE。
 * 4. tokenExpireTime 未配置时兜底 24 小时，不再因为 Integer 拆箱 NPE。
 */
@RestController
public class ApiLoginController extends BaseController {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final long DEFAULT_TOKEN_EXPIRE_HOURS = 24L;
    /** 密码长度限制，与前端 ChangePasswordDialog / LoginViews 的校验规则保持一致 */
    private static final int PASSWORD_MIN_LENGTH = 6;
    private static final int PASSWORD_MAX_LENGTH = 20;

    @Autowired
    StudentService studentService;

    @Autowired
    TeacherService teacherService;

    @Autowired
    PasswordUtil passwordUtil;

    @Autowired
    UserTokenService userTokenService;

    @Autowired
    ShiroProperties shiroProperties;

    @PostMapping("/api/login")
    public R login(@RequestBody Map<String, String> body) {
        String username = trimToNull(body == null ? null : body.get("username"));
        String password = body == null ? null : body.get("password");

        if (username == null || password == null || password.isEmpty()) {
            throw new ResultException(50100, "用户名或密码不能为空");
        }

        // 使用 hutool 工具类 IdUtil 生成 token
        String token = IdUtil.fastSimpleUUID();
        Subject subject = SecurityUtils.getSubject();

        QingjiaUsernamePasswordToken usernamePasswordToken = new QingjiaUsernamePasswordToken();
        usernamePasswordToken.setUsername(username);
        usernamePasswordToken.setPassword(password.toCharArray());

        Integer userId;
        int userType;

        if (username.length() > 7) {
            // 学生登录
            StudentPojo studentPojo = studentService.getByNum(username);
            if (studentPojo == null) {
                throw new ResultException(ResultEnum.LOGIN_STUDENT_NUM_NO_EXIST);
            }
            checkPassword(studentPojo.getSalt(), studentPojo.getPassword(), password);
            usernamePasswordToken.setType(UserTypeConstant.USER_TYPE_STUDENT);
            loginWithShiro(subject, usernamePasswordToken);
            userId = studentPojo.getId();
            userType = LoginUser.TYPE_STUDENT;
        } else {
            // 教师登录
            TeacherPojo teacherPojo = teacherService.getByNum(username);
            if (teacherPojo == null) {
                throw new ResultException(ResultEnum.LOGIN_TEACHER_NUM_NO_EXIST);
            }
            checkPassword(teacherPojo.getSalt(), teacherPojo.getPassword(), password);
            usernamePasswordToken.setType(UserTypeConstant.USER_TYPE_TEACHER);
            loginWithShiro(subject, usernamePasswordToken);
            userId = teacherPojo.getId();
            userType = LoginUser.TYPE_TEACHER;
        }

        // 认证成功后保存或更新 user_token 表
        userTokenService.saveOrUpdate(userId, userType, token, tokenExpireHours());

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("loginUserInfo", sanitizePrincipal(subject.getPrincipal()));
        return R.ok("登录成功").put("data", data);
    }

    /**
     * 退出登录：删除 user_token 记录并注销 shiro 会话。
     */
    @GetMapping("/api/logout")
    public R logout(HttpServletRequest request) {
        String token = resolveBearerToken(request);
        if (token != null) {
            UserTokenPojo userToken = userTokenService.getByToken(token);
            if (userToken != null) {
                userTokenService.deleteByToken(token);
            }
        }
        SecurityUtils.getSubject().logout();
        return R.ok("退出成功");
    }
    /**
     * 修改当前登录用户的密码（前端"用户名下拉框 -> 修改密码"调用）。
     *
     * 原实现（{@code @GetMapping("api/updatepassword")}）有四个必现问题，这里一并修掉：
     * 1. GET + @RequestBody：浏览器和 axios 都不会给 GET 带请求体，这个接口根本调不通。
     * 2. 判空条件写反了：{@code if (loginUser.getTeacher() == null)} 的分支里却调用
     *    {@code loginUser.getTeacher().getId()}，只要进这个分支必定 NPE；学生分支同理。
     * 3. {@code @RequestBody String} 收到的是整个 JSON 文本（含大括号和引号），
     *    会被当成密码原文加密写库，改完就再也登不进来了。
     * 4. 不校验原密码——任何拿到 token 的人都能直接改掉密码；
     *    改完也不作废旧 token，等于没改。
     */
    @PostMapping("/api/updatepassword")
    public R updatePassword(@RequestBody UpdatePasswordForm form, HttpServletRequest request) {
        LoginUser loginUser = getLoginUser();
        if (loginUser == null) {
            return R.error(401, "登录已过期，请重新登录");
        }

        String oldPassword = form == null ? null : form.getOldPassword();
        String newPassword = form == null ? null : form.getNewPassword();
        if (oldPassword == null || oldPassword.isEmpty()
                || newPassword == null || newPassword.trim().isEmpty()) {
            return R.error(50110, "原密码和新密码都不能为空");
        }
        if (newPassword.length() < PASSWORD_MIN_LENGTH || newPassword.length() > PASSWORD_MAX_LENGTH) {
            return R.error(50111, "新密码长度需在 " + PASSWORD_MIN_LENGTH
                    + " 到 " + PASSWORD_MAX_LENGTH + " 个字符之间");
        }
        if (oldPassword.equals(newPassword)) {
            return R.error(50112, "新密码不能与原密码相同");
        }

        boolean updated;
        if (loginUser.getType() == LoginUser.TYPE_TEACHER) {
            if (loginUser.getTeacher() == null) {
                return R.error(50113, "无法识别当前用户类型");
            }
            // 用库里的最新记录校验，session/principal 里的可能是登录时的旧快照
            TeacherPojo teacher = teacherService.getById(loginUser.getTeacher().getId());
            if (teacher == null) {
                return errorOf(ResultEnum.LOGIN_TEACHER_NUM_NO_EXIST);
            }
            if (!passwordUtil.matches(oldPassword, teacher.getPassword(), teacher.getSalt())) {
                return R.error(50114, "原密码不正确");
            }
            updated = teacherService.resetPassword(teacher.getId(), newPassword);
        } else if (loginUser.getType() == LoginUser.TYPE_STUDENT) {
            if (loginUser.getStudent() == null) {
                return R.error(50113, "无法识别当前用户类型");
            }
            StudentPojo student = studentService.getById(loginUser.getStudent().getId());
            if (student == null) {
                return errorOf(ResultEnum.STUDENT_NOT_EXIST);
            }
            if (!passwordUtil.matches(oldPassword, student.getPassword(), student.getSalt())) {
                return R.error(50114, "原密码不正确");
            }
            updated = studentService.resetPassword(student.getId(), newPassword);
        } else {
            return R.error(50113, "无法识别当前用户类型");
        }

        if (!updated) {
            return R.error(500, "密码修改失败，请稍后重试");
        }

        // 密码已变更：作废当前 token 并注销 shiro 会话，强制用新密码重新登录。
        // user_token 表按 (userId, userType) 只存一行，删掉这一行即该用户全端下线。
        String token = resolveBearerToken(request);
        if (token != null) {
            userTokenService.deleteByToken(token);
        }
        SecurityUtils.getSubject().logout();

        return R.ok("密码修改成功，请使用新密码重新登录");
    }

    private void checkPassword(String salt, String encryptedPassword, String rawPassword) {
        if (salt == null || salt.trim().isEmpty()
                || encryptedPassword == null || encryptedPassword.trim().isEmpty()) {
            throw new ResultException(50105, "该账号尚未初始化密码，请联系管理员重置密码");
        }
        if (!passwordUtil.matches(rawPassword, encryptedPassword, salt)) {
            throw new ResultException(ResultEnum.LOGIN_PWD_ERROR);
        }
    }

    /**
     * 在 Shiro 中完成认证；失败统一转成"密码不正确"业务错误，
     * 避免把 AuthenticationException 内部细节直接抛给前端。
     */
    private void loginWithShiro(Subject subject, QingjiaUsernamePasswordToken token) {
        try {
            subject.login(token);
        } catch (AuthenticationException e) {
            throw new ResultException(ResultEnum.LOGIN_PWD_ERROR);
        }
    }

    private long tokenExpireHours() {
        Integer configured = shiroProperties.getTokenExpireTime();
        if (configured == null || configured <= 0) {
            return DEFAULT_TOKEN_EXPIRE_HOURS;
        }
        return configured.longValue();
    }

    private static String resolveBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            String token = authorization.substring(BEARER_PREFIX.length()).trim();
            return token.isEmpty() ? null : token;
        }
        return null;
    }

    /**
     * 抹掉返回给前端的登录用户信息里的密码散列和盐。
     *
     * 原来直接把 subject.getPrincipal() 整个塞进响应体，LoginUser 里的
     * StudentPojo / TeacherPojo 会带着 password 和 salt 一起序列化出去 ——
     * 等于把库里的口令散列和盐交给任何登录成功的调用方，可以拿去离线爆破，
     * 而且前端 userStorage.set(userInfo) 还会把它存进浏览器 localStorage。
     * 前端 LoginUserInfo 只用到 type / student / teacher 的基本资料加上
     * roleNames / permissionNames，从来不需要这两个字段。
     *
     * 直接改 principal 里的对象是安全的：认证已经结束，后续鉴权只读
     * roleNames / permissionNames；Bearer 流程下每次请求都重新从库里构建 LoginUser，
     * 不存在把「抹掉密码的对象」缓存下来复用的情况。
     */
    private static LoginUser sanitizePrincipal(Object principal) {
        if (!(principal instanceof LoginUser)) {
            return null;
        }
        LoginUser loginUser = (LoginUser) principal;
        StudentPojo student = loginUser.getStudent();
        if (student != null) {
            student.setPassword(null);
            student.setSalt(null);
        }
        TeacherPojo teacher = loginUser.getTeacher();
        if (teacher != null) {
            teacher.setPassword(null);
            teacher.setSalt(null);
        }
        return loginUser;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static R errorOf(ResultEnum resultEnum) {
        return R.error(resultEnum.getStatus(), resultEnum.getMessage());
    }

    /**
     * 修改密码请求体。用显式的 getter/setter 而不是 Map，
     * 避免 @RequestBody String 那种把整段 JSON 当密码的问题。
     */
    public static class UpdatePasswordForm {
        private String oldPassword;
        private String newPassword;

        public String getOldPassword() {
            return oldPassword;
        }

        public void setOldPassword(String oldPassword) {
            this.oldPassword = oldPassword;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }
}
