package edu.hbmd.taotao.qingjia.shiro;

import cn.hutool.json.JSONUtil;
import edu.hbmd.taotao.qingjia.util.R;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.util.StringUtils;
import org.apache.shiro.web.filter.authc.AuthenticatingFilter;
import org.apache.shiro.web.util.WebUtils;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

public class QingjiaBearerFilter extends AuthenticatingFilter {
    // 请求头中Authorization字段的值前缀
    private static final String BEARER = "Bearer ";

    @Override
    protected AuthenticationToken createToken(ServletRequest request, ServletResponse response) throws Exception {
        HttpServletRequest httpRequest = WebUtils.toHttp(request);
        String authorization = httpRequest.getHeader("Authorization");
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER)) {
            return null; // 没有token，返回null，后续onAccessDenied会处理
        }
        String token = authorization.substring(BEARER.length()).trim();
        if (!StringUtils.hasText(token)) {
            return null;
        }
        return new QingjiaBearerToken(token);
    }

    @Override
    protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) {
        // 如果已经认证通过，则直接放行
        if (super.isAccessAllowed(request, response, mappedValue)) {
            return true;
        }
        // OPTIONS请求直接放行（跨域预检）
        HttpServletRequest httpRequest = WebUtils.toHttp(request);
        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            return true;
        }
        return false;
    }

    @Override
    protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {
        // 请求头里根本没有 Authorization 时，上面的 createToken 会返回 null。
        // 而 AuthenticatingFilter.executeLogin 遇到 null 不会走 onLoginFailure，
        // 它直接抛 IllegalStateException —— 异常冒泡到容器，被 forward 到 /error，
        // 再被 /** 上的 qingFilter 拦下 302 到 login.html。
        // 前端 axios 拿到的是一个跨域重定向后的 HTML，报的就是 Network Error，
        // request.ts 里 status===401 自动跳登录页的分支永远不会触发。
        // 所以这里先自己判空，直接回 401 JSON。
        if (createToken(request, response) == null) {
            writeUnauthorized(response);
            return false;
        }

        // token 存在但无效/过期：executeLogin 内部会走 onLoginFailure 返回 false
        if (executeLogin(request, response)) {
            return true;
        }

        writeUnauthorized(response);
        return false;
    }

    private void writeUnauthorized(ServletResponse response) throws java.io.IOException {
        HttpServletResponse httpResponse = WebUtils.toHttp(response);
        httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        httpResponse.setContentType("application/json;charset=UTF-8");
        PrintWriter writer = httpResponse.getWriter();
        R r = R.error(401, "未认证或Token无效，请重新登录");
        writer.write(JSONUtil.toJsonStr(r));
        writer.flush();
    }

    @Override
    protected boolean onLoginFailure(AuthenticationToken token,
                                     AuthenticationException e, ServletRequest request, ServletResponse response) {
        // 登录失败由onAccessDenied处理，这里不再重复输出
        return false;
    }
}
