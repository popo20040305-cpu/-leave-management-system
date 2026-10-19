package edu.hbmd.taotao.qingjia.config;

import cn.hutool.json.JSONUtil;
import edu.hbmd.taotao.qingjia.util.JwtUtils;
import edu.hbmd.taotao.qingjia.util.R;
import org.apache.shiro.SecurityUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

@Component
public class JwtInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;   // 跨域预检放行

        // ① 老页面走 Shiro session 的，照常放行
        try {
            if (SecurityUtils.getSubject().isAuthenticated()) return true;
        } catch (Exception ignore) {}

        // ② 否则必须带有效 JWT
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) token = token.substring(7);
        if (token != null && JwtUtils.verify(token)) {
            request.setAttribute("loginUser", JwtUtils.parse(token).getPayload("username"));
            request.setAttribute("loginUserType", JwtUtils.parse(token).getPayload("userType"));
            return true;
        }

        // ③ 都没有 → 401
        response.setStatus(401);
        response.setContentType("application/json;charset=utf-8");
        PrintWriter out = response.getWriter();
        out.write(JSONUtil.toJsonStr(R.error(401, "未登录或登录已过期，请重新登录")));
        return false;
    }
}