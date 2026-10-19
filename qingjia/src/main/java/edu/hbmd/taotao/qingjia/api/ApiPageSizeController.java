package edu.hbmd.taotao.qingjia.api;

import edu.hbmd.taotao.qingjia.constant.PageConstant;
import edu.hbmd.taotao.qingjia.shiro.LoginUser;
import edu.hbmd.taotao.qingjia.util.R;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 按登录用户记忆每页条数（文档 10.2 节）。
 *
 * 值不能放 Shiro session：前端走的是 Bearer token（JwtFilter），不是 Cookie 会话，
 * 浏览器不会回传 JSESSIONID，subject.getSession() 每次请求都是一个新的匿名 session，
 * 写进去下次就读不到（联调实测 size=20 存完立刻读回默认 10）。
 * 所以这里改成按「用户类型 + 用户 id」的进程内缓存，登录期间一直有效，
 * 语义上等价于文档里的 session 记忆（Shiro session 默认也是内存态、重启即失）。
 *
 * BaseController.getPageSize() 会调用 getPageSizeOfCurrentUser() 读这个值。
 */
@RestController
@RequestMapping("/api")
public class ApiPageSizeController {

    /** 单页条数上限，防止误传一个超大值把整张表捞出来 */
    private static final int MAX_PAGE_SIZE = 200;

    /**
     * key = userType + ":" + userId（教师和学生的 id 各自独立编号，必须带上类型区分）。
     * value = 每页条数。
     */
    private static final Map<String, Integer> PAGE_SIZE_CACHE = new ConcurrentHashMap<>();

    @GetMapping("/setPageSize")
    public R setPageSize(@RequestParam Integer pageSize) {
        Subject subject = SecurityUtils.getSubject();
        LoginUser loginUser = (LoginUser) subject.getPrincipal();
        if (loginUser == null) {
            return R.error(401, "未登录");
        }
        int size = (pageSize == null || pageSize <= 0) ? PageConstant.pageSize : pageSize;
        if (size > MAX_PAGE_SIZE) {
            size = MAX_PAGE_SIZE;
        }
        PAGE_SIZE_CACHE.put(cacheKey(loginUser), size);
        return R.ok("页面大小设置成功");
    }

    /**
     * 读取当前登录用户记住的每页条数，没有设置过返回 null。
     * 供 BaseController.getPageSize() 在「请求没显式传 size」时兜底。
     */
    public static Integer getPageSizeOfCurrentUser() {
        Subject subject = SecurityUtils.getSubject();
        if (subject == null) {
            return null;
        }
        Object principal = subject.getPrincipal();
        if (!(principal instanceof LoginUser)) {
            return null;
        }
        return PAGE_SIZE_CACHE.get(cacheKey((LoginUser) principal));
    }

    private static String cacheKey(LoginUser loginUser) {
        return loginUser.getType() + ":" + currentUserId(loginUser);
    }

    private static Integer currentUserId(LoginUser loginUser) {
        if (loginUser.getStudent() != null) {
            return loginUser.getStudent().getId();
        }
        return loginUser.getTeacher().getId();
    }
}
