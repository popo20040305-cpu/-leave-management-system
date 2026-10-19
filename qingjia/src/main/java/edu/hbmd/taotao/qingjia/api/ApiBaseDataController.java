package edu.hbmd.taotao.qingjia.api;

import edu.hbmd.taotao.qingjia.controller.BaseController;
import edu.hbmd.taotao.qingjia.service.ApplicationStatusService;
import edu.hbmd.taotao.qingjia.service.AproveTypeService;
import edu.hbmd.taotao.qingjia.service.GradeService;
import edu.hbmd.taotao.qingjia.service.LeaveApplicationTypeService;
import edu.hbmd.taotao.qingjia.service.MenuTypeService;
import edu.hbmd.taotao.qingjia.service.RoleTypeService;
import edu.hbmd.taotao.qingjia.service.SexTypeService;
import edu.hbmd.taotao.qingjia.util.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 公共基础数据（字典）接口，供 Vue 前端启动时一次性拉取。
 *
 * 对应老项目里 BaseController + DataInitializer 那套：老页面每次渲染时
 * 从 ServletContext 里取 colleges / grades / sexTypes... 塞进 Model；
 * 前后端分离之后改成前端启动时调一次、存进 Pinia 复用。
 *
 * 设计要点：
 * 1. 一个 /all 接口返回全部字典，避免页面初始化时打七八个请求。
 *    带 ?keys=colleges,sexTypes 可以只取子集，用于某个字典增删改后的局部刷新。
 * 2. 学院列表按登录用户过滤（学生只看自己班级所属学院、教师只看自己学院、
 *    ROLE_TYPE_ALL 看全部），规则和 BaseController.getCollegeByLoginUser() 一致。
 *    所以这个接口必须登录后才能调 —— 它挂在 /api/** 下，由 bearerFilter 保护。
 * 3. 一律直接查库，不读 ServletContext 缓存。那份缓存启动后没有任何地方刷新，
 *    新增学院要重启才生效。字典表都只有几行，查库成本可以忽略。
 */
@RestController
@RequestMapping("/api/base")
public class ApiBaseDataController extends BaseController {

    /** 支持的字典 key，这里的顺序就是返回 JSON 里的字段顺序 */
    private static final List<String> ALL_KEYS = Collections.unmodifiableList(Arrays.asList(
            "colleges",
            "grades",
            "aproveTypes",
            "leaveApplicationTypes",
            "menuTypes",
            "roleTypes",
            "sexTypes",
            "applicationStatuses"
    ));

    @Autowired
    private GradeService gradeService;
    @Autowired
    private AproveTypeService aproveTypeService;
    @Autowired
    private LeaveApplicationTypeService leaveApplicationTypeService;
    @Autowired
    private MenuTypeService menuTypeService;
    @Autowired
    private RoleTypeService roleTypeService;
    @Autowired
    private SexTypeService sexTypeService;
    @Autowired
    private ApplicationStatusService applicationStatusService;

    /**
     * 拉取基础数据。
     *
     * @param keys 逗号分隔的字典 key，例如 colleges,sexTypes；不传则返回全部
     */
    @GetMapping("/all")
    public R all(@RequestParam(value = "keys", required = false) String keys) {
        List<String> wanted = parseKeys(keys);

        // 先把 key 全部校验一遍再查，避免查了一半才发现拼错、返回半个残缺对象
        for (String key : wanted) {
            if (!ALL_KEYS.contains(key)) {
                return R.error(400, "未知的基础数据 key：" + key + "，可选值：" + ALL_KEYS);
            }
        }

        // LinkedHashMap 保证字段顺序稳定，前端调试和快照对比都不会乱跳
        Map<String, Object> data = new LinkedHashMap<>();
        for (String key : wanted) {
            data.put(key, loadByKey(key));
        }
        return R.ok().put("data", data);
    }

    private List<String> parseKeys(String keys) {
        if (keys == null || keys.trim().isEmpty()) {
            return ALL_KEYS;
        }
        List<String> result = new ArrayList<>();
        for (String part : keys.split(",")) {
            String key = part.trim();
            // 去重：keys=colleges,colleges 不该让学院被查两遍
            if (!key.isEmpty() && !result.contains(key)) {
                result.add(key);
            }
        }
        return result.isEmpty() ? ALL_KEYS : result;
    }

    private List<?> loadByKey(String key) {
        switch (key) {
            case "colleges":
                return getCollegesByLoginUserFromDb();
            case "grades":
                return gradeService.getAll();
            case "aproveTypes":
                return aproveTypeService.getAll();
            case "leaveApplicationTypes":
                return leaveApplicationTypeService.getAll();
            case "menuTypes":
                return menuTypeService.getAll();
            case "roleTypes":
                return roleTypeService.getAll();
            case "sexTypes":
                return sexTypeService.getAll();
            case "applicationStatuses":
                return applicationStatusService.list();
            default:
                // parseKeys 已经拦掉了未知 key，这个分支只是让编译器满意
                return Collections.emptyList();
        }
    }
}
