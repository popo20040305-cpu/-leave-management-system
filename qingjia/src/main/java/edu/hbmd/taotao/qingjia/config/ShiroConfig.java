package edu.hbmd.taotao.qingjia.config;

import at.pollux.thymeleaf.shiro.dialect.ShiroDialect;
import edu.hbmd.taotao.qingjia.shiro.*;
import org.apache.shiro.cache.CacheManager;
import org.apache.shiro.cache.MemoryConstrainedCacheManager;
import org.apache.shiro.realm.Realm;  // ✅ 修正：使用 Shiro 的 Realm
import org.apache.shiro.session.mgt.SessionManager;
import org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import javax.servlet.Filter;  // ✅ 修正：使用 Servlet Filter
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shiro 配置类
 */
@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class ShiroConfig {

    /**
     * 解决 spring-boot-starter-aop 与 Shiro 注解的冲突
     */
    @Bean
    public static DefaultAdvisorAutoProxyCreator defaultAdvisorAutoProxyCreator() {
        DefaultAdvisorAutoProxyCreator creator = new DefaultAdvisorAutoProxyCreator();
        // 强制使用CGLIB代理，这对没有实现接口的Controller至关重要
        creator.setProxyTargetClass(true);
        // 设置为true，有助于解决AOP环境下Controller映射失败的bug
        creator.setUsePrefix(true);
        return creator;
    }

    /**
     * Shiro 缓存管理器（使用内存缓存）
     */
    @Bean("shiroCacheManager")
    public CacheManager shiroCacheManager() {
        return new MemoryConstrainedCacheManager();
    }

    /**
     * 自定义凭证匹配器（用于 QingjiaRealm）
     */
    @Bean("qingjiaCredentialsMatcher")
    public QingjiaCredentialsMatcher getCredentialsMatcher() {
        return new QingjiaCredentialsMatcher();
    }

    /**
     * QingjiaRealm（用户名/密码认证）
     */
    @Bean("qingjiaRealm")
    public Realm qingjiaRealm(@Qualifier("shiroCacheManager") CacheManager cacheManager,
                              @Qualifier("qingjiaCredentialsMatcher") QingjiaCredentialsMatcher matcher) {
        QingjiaRealm realm = new QingjiaRealm();
        realm.setCacheManager(cacheManager);
        realm.setCachingEnabled(true);
        realm.setAuthorizationCachingEnabled(true);
        realm.setCredentialsMatcher(matcher);
        return realm;
    }

    /**
     * BearerRealm（Token认证）
     */
    @Bean("bearerRealm")
    public QingjiaBearerRealm bearerRealm(@Qualifier("shiroCacheManager") CacheManager cacheManager) {
        QingjiaBearerRealm realm = new QingjiaBearerRealm();
        realm.setCacheManager(cacheManager);
        realm.setCachingEnabled(true);
        realm.setAuthorizationCachingEnabled(true);
        // BearerRealm 不需要凭证匹配器，因为直接比较 token 字符串
        return realm;
    }

    /**
     * SessionManager 配置
     */
    @Bean("sessionManager")
    public SessionManager sessionManager() {
        DefaultWebSessionManager sessionManager = new DefaultWebSessionManager();
        // 禁用URL重写（不在URL后追加 jessionid）
        sessionManager.setSessionIdUrlRewritingEnabled(false);
        // 可选配置（按需启用）
        // sessionManager.setGlobalSessionTimeout(1800000);       // 30分钟
        // sessionManager.setSessionIdCookieEnabled(true);
        // sessionManager.setDeleteInvalidSessions(true);
        return sessionManager;
    }

    /**
     * SecurityManager 配置（多 Realm）
     */
    @Bean("securityManager")
    public DefaultWebSecurityManager getSecurityManager(
            @Qualifier("qingjiaRealm") Realm qingjiaRealm,
            @Qualifier("bearerRealm") Realm bearerRealm,
            @Qualifier("shiroCacheManager") CacheManager cacheManager,
            @Qualifier("sessionManager") SessionManager sessionManager) {
        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
        securityManager.setCacheManager(cacheManager);
        // 设置多个Realm
        List<Realm> realms = new ArrayList<>();
        realms.add(qingjiaRealm);
        realms.add(bearerRealm);
        securityManager.setRealms(realms);
        securityManager.setSessionManager(sessionManager);
        return securityManager;
    }

    /**
     * Shiro 过滤器工厂
     */
    @Bean("shiroFilterFactoryBean")
    public ShiroFilterFactoryBean getShiroFilterFactoryBean(
            @Qualifier("securityManager") DefaultWebSecurityManager securityManager,
            ShiroProperties shiroProperties) {
        ShiroFilterFactoryBean factoryBean = new ShiroFilterFactoryBean();
        factoryBean.setSecurityManager(securityManager);

        // 配置自定义过滤器
        Map<String, Filter> filters = factoryBean.getFilters();
        filters.put("qingFilter", new QingjiaFilter());
        filters.put("bearerFilter", new QingjiaBearerFilter());

        // 配置过滤链
        Map<String, String> filterChain = new LinkedHashMap<>();

        // 1. 放行路径（从配置中读取）
        for (String url : shiroProperties.getExcludePathPatterns()) {
            filterChain.put(url, "anon");
        }

        // 2. 所有 /api/** 请求使用 Bearer Token 认证
        filterChain.put("/api/**", "bearerFilter");

        // 3. 其余所有请求（如页面）使用原有的表单认证
        filterChain.put("/**", "qingFilter");

        factoryBean.setFilterChainDefinitionMap(filterChain);

        // 设置登录、成功、未授权页面
        factoryBean.setLoginUrl(shiroProperties.getLoginUrl());
        factoryBean.setSuccessUrl(shiroProperties.getLoginSuccessUrl());
        factoryBean.setUnauthorizedUrl(shiroProperties.getUnauthorizedUrl());

        return factoryBean;
    }

    /**
     * 启用 Shiro 授权注解拦截（AOP 方法级权限检查）
     */
    @Bean
    public AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor(
            @Qualifier("securityManager") DefaultWebSecurityManager securityManager) {
        AuthorizationAttributeSourceAdvisor advisor = new AuthorizationAttributeSourceAdvisor();
        advisor.setSecurityManager(securityManager);
        return advisor;
    }

    /**
     * 启用 Shiro 标签方言（用于 Thymeleaf 等视图模板）
     */
    @Bean
    public ShiroDialect shiroDialect() {
        return new ShiroDialect();
    }
}
