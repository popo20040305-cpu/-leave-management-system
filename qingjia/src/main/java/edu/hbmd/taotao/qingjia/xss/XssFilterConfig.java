package edu.hbmd.taotao.qingjia.xss;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.Filter;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class XssFilterConfig {
    private static final int FILTER_ORDER = 1;
    @Bean
    public FilterRegistrationBean xssFilterRegistrationBean(XssProperties propertiesXxs) {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<Filter>();
        XssFilter xssFilter = new XssFilter();
        xssFilter.setXssProperties(propertiesXxs);
        registration.setFilter(xssFilter);
        registration.setOrder(FILTER_ORDER);
        registration.setEnabled(propertiesXxs.isEnabled());
        registration.addUrlPatterns(propertiesXxs.getUrlPatterns().split(","));
        Map<String, String> initParameters = new HashMap<>(16);
        initParameters.put("excludes", propertiesXxs.getExcludes());
        registration.setInitParameters(initParameters);
        return registration;
    }

}
