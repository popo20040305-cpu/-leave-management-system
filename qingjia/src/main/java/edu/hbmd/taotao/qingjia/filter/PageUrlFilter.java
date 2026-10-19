package edu.hbmd.taotao.qingjia.filter;

import org.springframework.web.servlet.HandlerInterceptor;

import javax.lang.model.element.NestingKind;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;

public class PageUrlFilter implements HandlerInterceptor  {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取分页参数地址
        if (request.getParameter("curPage")!=null){
            StringBuffer urlBuffer = request.getRequestURL();
            urlBuffer.append("?");
            Enumeration em = request.getParameterNames();
            String curPage = "";
            while (em.hasMoreElements()) {
                String name = (String) em.nextElement();
                if(!"curPage".equals(name)){
                    String value = request.getParameter(name);
                    urlBuffer.append(name + "=" + value + "&");
                }
            }
            urlBuffer.append("curPage=");
            request.setAttribute("pageUrl",urlBuffer.toString());
        }
        return true;
    }

}
