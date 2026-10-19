package edu.hbmd.taotao.qingjia.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

/**
 * 跨域配置。
 *
 * 修复点：
 * 1. 必须使用 Spring 的 org.springframework.web.filter.CorsFilter，并把构造好的
 *    UrlBasedCorsConfigurationSource 传进去。原来写的是 Tomcat 的
 *    org.apache.catalina.filters.CorsFilter 无参构造，CorsConfiguration 被整个丢弃，
 *    导致所有带 Origin 的请求（含 OPTIONS 预检）一律 403。
 * 2. 用 FilterRegistrationBean 排到最高优先级，保证在 Shiro / XSS 过滤器之前执行，
 *    预检请求才能被正确应答，Shiro 拒绝时响应里也带得上 CORS 头。
 */
@Configuration
public class CorsConfig {

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilterRegistrationBean() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(
                "http://localhost:5173",
                "http://127.0.0.1:5173"
        ));
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        FilterRegistrationBean<CorsFilter> registration = new FilterRegistrationBean<>(new CorsFilter(source));
        registration.setName("corsFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
