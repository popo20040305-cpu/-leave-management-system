package edu.hbmd.taotao.qingjia.xss;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * xss防护设置
 */
@Data
@Component
@ConfigurationProperties(prefix = "project.xss")
public class XssProperties {
    /** xss防护开关 */
    private boolean enabled = true;

    //白名单类型：none 不防护  simpleText 文本  basic 基本防护  basicWithImages基本与图片 relaxed 宽松
    private String whiteList = "none";
    /** 拦截规则，可通过“,”隔开多个 */
    private String urlPatterns = "/*";
    /** 默认忽略规则（无需修改） */
    private String defaultExcludes = "/favicon.ico,/images/*,/js/*,/css/*,/lib/*";

    /** 忽略规则，可通过“,”隔开多个 */
    private String excludes = "";

    /**
     * 拼接忽略规则
     */
    public String getExcludes() {
        if (!StrUtil.isEmpty(excludes.trim())) {
            return defaultExcludes + "," + excludes;
        }
        return defaultExcludes;
    }

}
