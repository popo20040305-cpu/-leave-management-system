package edu.hbmd.taotao.qingjia.controller;

import edu.hbmd.taotao.qingjia.util.R;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 自定义错误控制器，完全接管 /error 请求。
 * 根据客户端 Accept 头，返回 HTML 页面或 JSON 数据。
 * 兼容 Spring Boot 2.2.2（手动处理 errors 列表）。
 */
@Controller
public class MyErrorController implements ErrorController {
    // 错误处理路径，与 Spring Boot 默认的 /error 保持一致
    private static final String ERROR_PATH_HTML = "/errorHtml";
    private static final String ERROR_PATH_AJAX = "/errorJson";


    // Spring Boot 提供的错误属性提取器，可以从 request 中解析出标准错误属性
    private final ErrorAttributes errorAttributes;

    public MyErrorController(ErrorAttributes errorAttributes) {
        this.errorAttributes = errorAttributes;
    }

    /**
     * 处理浏览器请求（预期返回 HTML 页面）
     * 由于没有指定 produces，默认会处理 Accept 头包含 text/html 的请求
     */
    @RequestMapping(ERROR_PATH_HTML)
    public ModelAndView errorHtml(HttpServletRequest request, HttpServletResponse response) {
        // 将 HttpServletRequest 包装成 WebRequest，便于 ErrorAttributes 使用
        WebRequest webRequest = new ServletWebRequest(request);

        // Spring Boot 2.2.2 中 getErrorAttributes 的第二个参数为 boolean，true 表示包含异常堆栈信息
        Map<String, Object> errorAttrs = errorAttributes.getErrorAttributes(webRequest, false);

        // 获取 HTTP 状态码（之前由异常处理器设置到 request 属性中）
        HttpStatus status = getStatus(request);
        response.setStatus(status.value());

        // 构建视图：根据状态码返回不同的错误页面，例如 error/400.html、error/404.html、error/500.html
        // 注意：需要在 src/main/resources/templates/error/ 目录下放置对应的模板文件
        ModelAndView mv = new ModelAndView("error/" + status.value());

        // 将基本的错误信息添加到模型中，供页面展示
        mv.addObject("status", status.value());                       // HTTP 状态码，如 400, 404, 500
        mv.addObject("error", errorAttrs.get("error"));              // 错误类型，如 "Bad Request"
        mv.addObject("message", errorAttrs.get("message"));          // 错误消息
        mv.addObject("timestamp", errorAttrs.get("timestamp"));      // 错误发生时间
        mv.addObject("path", errorAttrs.get("path"));                // 引起错误的原始请求路径
        //JRS303错误处理
        Object errors = request.getAttribute("errors");
        if (errors != null) {
            mv.addObject("errors", errors);
        }

        // 如果有自定义业务状态码（例如从 ResultException 中带来的 50101 等），也添加到模型
        Object businessCode = request.getAttribute("business_status");
        if (businessCode != null) {
            mv.addObject("businessCode", businessCode);
        }

        return mv;
    }

    /**
     * 处理 AJAX / API 请求（客户端期望返回 JSON）
     * 通过 produces = "application/json" 指定只处理 Accept 头包含 application/json 的请求
     */
    @RequestMapping(ERROR_PATH_AJAX)
    @ResponseBody
    public R errorJson(HttpServletRequest request) {
        WebRequest webRequest = new ServletWebRequest(request);
        Map<String, Object> errorAttrs = errorAttributes.getErrorAttributes(webRequest, true);

        StringBuffer errorMessageBuf = new StringBuffer();
        // 修复：优先使用全局异常处理器写入的真实业务提示。
        // 在 @ControllerAdvice -> forward:/errorJson 这条链路上，
        // errorAttrs.get("message") 往往是 null，直接 append 会让前端收到字符串 "null"。
        Object attrMessage = request.getAttribute("javax.servlet.error.message");
        Object eaMessage = errorAttrs.get("message");
        if (attrMessage != null && String.valueOf(attrMessage).trim().length() > 0) {
            errorMessageBuf.append(attrMessage);
        } else if (eaMessage != null && String.valueOf(eaMessage).trim().length() > 0) {
            errorMessageBuf.append(eaMessage);
        } else {
            errorMessageBuf.append("请求处理失败");
        }
        // 手动提取JRS303校验错误列表，并转换为前端易于解析的格式
        List<FieldError> fieldErrors = (List<FieldError>) request.getAttribute("errors");
        if (fieldErrors != null && !fieldErrors.isEmpty()) {
            for (FieldError fe : fieldErrors) {
                errorMessageBuf.append("\n");
                errorMessageBuf.append(fe.getDefaultMessage());
            }
        }

        // 业务状态码原样带回给前端，前端响应拦截器据 code !== 200 判定失败
        Object businessCode = request.getAttribute("business_status");
        int code = (businessCode instanceof Integer) ? (Integer) businessCode : 500;

        return R.error(code, errorMessageBuf.toString());
    }


    /**
     * 从 request 属性中获取 HTTP 状态码，并转换为 HttpStatus 枚举。
     * 如果获取不到或转换失败，默认返回 500。
     *
     * @param request 当前请求
     * @return HttpStatus 枚举值
     */
    private HttpStatus getStatus(HttpServletRequest request) {
        Integer statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
        if (statusCode == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        try {
            return HttpStatus.valueOf(statusCode);
        } catch (Exception ex) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }

    /**
     * 实现 ErrorController 接口要求的方法，返回错误处理路径。
     * 虽然 Spring Boot 2.3+ 已弃用此方法，但在 2.2.2 中依然需要。
     */
    @Override
    public String getErrorPath() {
        return ERROR_PATH_HTML;
    }

}
