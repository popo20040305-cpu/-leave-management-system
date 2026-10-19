package edu.hbmd.taotao.qingjia.shiro;

import cn.hutool.json.JSONUtil;
import edu.hbmd.taotao.qingjia.constant.UserTypeConstant;
import edu.hbmd.taotao.qingjia.util.JwtUtils;
import edu.hbmd.taotao.qingjia.util.R;
import lombok.SneakyThrows;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.authc.AuthenticatingFilter;
import org.apache.shiro.web.util.WebUtils;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class QingjiaFilter extends AuthenticatingFilter {
    @Override
    protected AuthenticationToken createToken(ServletRequest servletRequest, ServletResponse servletResponse) throws Exception {
        //1、判断是学生登录还是老师登录
        String username = (String) servletRequest.getParameter("username");
        String password = (String) servletRequest.getParameter("password");
        QingjiaUsernamePasswordToken token = new QingjiaUsernamePasswordToken();
        if (username.length() > 7){
            //学生shiro登录
            token.setUsername(username);
            token.setPassword(password.toCharArray());
            token.setType(UserTypeConstant.USER_TYPE_STUDENT);
            token.setRememberMe(false);
        }else{
//学生shiro登录
            token.setUsername(username);
            token.setPassword(password.toCharArray());
            token.setType(UserTypeConstant.USER_TYPE_TEACHER);
            token.setRememberMe(false);
        }
        return token;
    }

    @SneakyThrows
    @Override
    protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) {
        if (super.isAccessAllowed(request, response, mappedValue)){
            if (this.isLoginRequest(request, response)){
                WebUtils.redirectToSavedRequest(request, response, "/");
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    protected boolean onAccessDenied(ServletRequest servletRequest, ServletResponse servletResponse)  {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        System.out.println(request.getRequestURL());
        if (this.isLoginRequest(servletRequest, servletResponse)){
            if (this.isPost(servletRequest)){
                try {
                    return executeLogin(servletRequest,servletResponse);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return true;
        }
        try {
            saveRequestAndRedirectToLogin(servletRequest,servletResponse);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    protected boolean onLoginSuccess(AuthenticationToken token, Subject subject,
                                     ServletRequest request, ServletResponse response) throws Exception {
        HttpServletRequest req = (HttpServletRequest) request;
        if (isAjaxRequest(req)) {   // 前端 ajax 登录 → 返回 JSON + token
            QingjiaUsernamePasswordToken t = (QingjiaUsernamePasswordToken) token;
            String jwt = JwtUtils.createToken(t.getUsername(), t.getType());
            HttpServletResponse resp = (HttpServletResponse) response;
            resp.setContentType("application/json;charset=utf-8");
            resp.setHeader("Authorization", jwt);
            Map<String, Object> data = new HashMap<>();
            data.put("token", jwt);
            resp.getWriter().write(JSONUtil.toJsonStr(R.ok(data)));
            return false;
        }
        WebUtils.issueRedirect(request, response, "/");   // 老页面走原逻辑
        return false;
    }

    @Override
    protected boolean onLoginFailure(AuthenticationToken token, AuthenticationException e, ServletRequest request, ServletResponse response) {
        HttpServletRequest httpRequest = WebUtils.toHttp(request);
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        if (isAjaxRequest(httpRequest)) {
// 添加CORS头
            httpResponse.setHeader("Access-Control-Allow-Origin", "*");
            httpResponse.setHeader("Access-Control-Allow-Methods", "GET,POST,OPTIONS,PUT,DELETE");
            httpResponse.setHeader("Access-Control-Allow-Headers", "X-Requested-With, Content-Type, Authorization");
            httpResponse.setHeader("Access-Control-Allow-Credentials", "true");
            // ajax访问，返回 JSON 错误信息
            response.setContentType("application/json; charset=utf-8");
            try {
                PrintWriter writer = response.getWriter();
                R r = R.error(403,"密码错误，无权访问，请重新登录");
                writer.write(JSONUtil.toJsonStr(r));
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        } else {//浏览器访问
            try {
                WebUtils.issueRedirect(request, response, "/login.html");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return false;
    }

    private boolean isPost(ServletRequest request) {
        return ((HttpServletRequest) request).getMethod().equalsIgnoreCase("POST");
    }

    private boolean isAjaxRequest(HttpServletRequest request) {
        //跨域方式检查
        if ("jquery".equals(request.getParameter("request_source"))){
            return true;
        }
        // 1. 检查路径是否以/api开头（约定）
        if (request.getRequestURI().startsWith("/api/")) {
            return true;
        }

        // 2. 检查X-Requested-With头
        String requestedWith = request.getHeader("X-Requested-With");
        if (requestedWith != null && "XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
            return true;
        }

        // 3. 检查Accept头
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            return true;
        }

        // 4. 检查Content-Type头
        String contentType = request.getHeader("Content-Type");
        if (contentType != null && contentType.contains("application/json")) {
            return true;
        }

        // 5. 检查请求来源
        String referer = request.getHeader("Referer");
        String servletPath = request.getServletPath();//servletpath以"api开头"
        if (referer == null && servletPath.startsWith("/api")) {
            return true; // 没有来源页可能是API调用
        }

        return false;
    }
}
