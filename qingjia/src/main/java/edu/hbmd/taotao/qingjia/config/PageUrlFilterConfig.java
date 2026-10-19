package edu.hbmd.taotao.qingjia.config;

import edu.hbmd.taotao.qingjia.filter.PageUrlFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class PageUrlFilterConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new PageUrlFilter())
                .addPathPatterns("/**")
                .excludePathPatterns("/webjars/**")
                .excludePathPatterns("/images/**")
                .excludePathPatterns("/teacherPhoto/**")
                .excludePathPatterns("/studentPhoto/**")
                .excludePathPatterns("/xiaojiaPhoto/**")
                .excludePathPatterns("/js/**")
                .excludePathPatterns("/css/**");

        // jwt 拦截器
        // 修复点：必须放行登录/登出接口本身，否则没登录时会被它 401 掉；
        // 同时放行 /errorJson、/errorHtml，否则全局异常处理器 forward 过去之后
        // 会被这个拦截器改写成 401，前端永远看不到真正的业务错误提示。
        //
        // 另外必须放行上传的图片目录（/teacherPhoto/** 等）：浏览器加载
        // <img src="/teacherPhoto/xxx.jpg"> 是普通 GET，不会带 Authorization 头，
        // 被这个拦截器拦下就是 401 + 一段 JSON，图片永远显示不出来。
        // 注意 Shiro 那边（application.yml 的 exclude-path-patterns）也要放行，
        // 两处配置是各自独立的过滤链，缺任一处都还是拿不到图片。
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/api/login")
                .excludePathPatterns("/api/logout")
                .excludePathPatterns("/login.html")
                .excludePathPatterns("/logout.html")
                .excludePathPatterns("/captcha")
                .excludePathPatterns("/errorJson")
                .excludePathPatterns("/errorHtml")
                .excludePathPatterns("/webjars/**", "/images/**", "/js/**", "/css/**")
                .excludePathPatterns("/teacherPhoto/**")
                .excludePathPatterns("/studentPhoto/**")
                .excludePathPatterns("/xiaojiaPhoto/**");
    }
}
