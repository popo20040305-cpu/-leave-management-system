package edu.hbmd.taotao.qingjia.exception;

import edu.hbmd.taotao.qingjia.controller.MyErrorController;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.AuthorizationException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

/**
 * 全局统一异常处理
 * @author 陶勇
 * @date 2024/4/20
 */
@ControllerAdvice
@Slf4j
public class ResultExceptionHandler {

    /**
     * 处理自定义业务异常 ResultException
     * 例如：学号不存在、密码错误、角色已存在等业务上的逻辑错误
     */
    @ExceptionHandler(ResultException.class)
    public String handleResultException(ResultException e, HttpServletRequest request) {
        log.warn("业务异常：状态码={}, 消息={}", e.getStatus(), e.getMessage());

        // 将业务状态码存入 request，后续可以在 MyErrorController 中取出
        request.setAttribute("business_status", e.getStatus());

        // 设置 HTTP 状态码（需要根据业务状态码映射到合理的 HTTP 状态码）
        // 此处简单处理：如果是 4xx 范围内的业务码，返回 400；否则返回 500
        int httpStatus = (e.getStatus() >= 400 && e.getStatus() < 500) ? 400 : 500;
        request.setAttribute("javax.servlet.error.status_code", httpStatus);

        // 设置错误消息（直接使用异常中的消息）
        request.setAttribute("javax.servlet.error.message", e.getMessage());

        // 将原始异常对象也存进去，便于后续提取详细信息（例如堆栈）
        request.setAttribute("javax.servlet.error.exception", e);

        // 转发到 /error 端点，由 MyErrorController 接手
        String accept = request.getHeader("accept");
        if (accept == null) { accept = ""; } // 修复：无 Accept 头时原代码会 NPE
        if (accept.contains("text/html,")){
            return "forward:/errorHtml";
        }else
        {
            return "forward:/errorJson";
        }
    }

    /**
     * 处理 JSR303 参数校验失败异常（@Valid 校验不通过时抛出）
     * 例如：@NotBlank、@Min、@Max 等校验失败
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public String handleMethodArgumentNotValid(MethodArgumentNotValidException e, HttpServletRequest request) {
        log.warn("参数校验失败：{}", e.getMessage());

        // 参数校验错误属于客户端错误，HTTP 状态码使用 400
        request.setAttribute("javax.servlet.error.status_code", HttpStatus.BAD_REQUEST.value());

        // 设置一个简明的提示信息
        request.setAttribute("javax.servlet.error.message", "请求参数校验失败，请检查输入");

        // 将原始异常对象存入 request，后续 MyErrorController 会从中提取字段错误列表（errors）
        request.setAttribute("javax.servlet.error.exception", e);

        // 保存JSR303 校验错误列表，直接将 BindingResult 中的错误列表单独存一份（可选，便于后续直接获取）
        request.setAttribute("errors", e.getBindingResult().getFieldErrors());

        // 转发到 /error 端点，由 MyErrorController 接手
        String accept = request.getHeader("accept");
        if (accept == null) { accept = ""; } // 修复：无 Accept 头时原代码会 NPE
        if (accept.contains("text/html,")){
            return "forward:/errorHtml";
        }else
        {
            return "forward:/errorJson";
        }
    }

    /**
     * 处理表单绑定异常（使用 @ModelAttribute + @Valid 且没有 @RequestBody 时可能抛出）
     * 和 MethodArgumentNotValidException 类似，也需要提取字段错误
     */
    @ExceptionHandler(BindException.class)
    public String handleBindException(BindException e, HttpServletRequest request) {
        log.warn("表单绑定校验失败：{}", e.getMessage());

        request.setAttribute("javax.servlet.error.status_code", HttpStatus.BAD_REQUEST.value());
        request.setAttribute("javax.servlet.error.message", "表单参数校验失败");
        request.setAttribute("javax.servlet.error.exception", e);
        // 保存JSR303 校验错误列表，直接将 BindingResult 中的错误列表单独存一份（可选，便于后续直接获取）
        request.setAttribute("errors", e.getBindingResult().getFieldErrors());

        // 转发到 /error 端点，由 MyErrorController 接手
        String accept = request.getHeader("accept");
        if (accept == null) { accept = ""; } // 修复：无 Accept 头时原代码会 NPE
        if (accept.contains("text/html,")){
            return "forward:/errorHtml";
        }else
        {
            return "forward:/errorJson";
        }
    }

    /**
     * 处理 Shiro 授权失败（@RequiresPermissions / @RequiresRoles 校验不通过）
     */
    @ExceptionHandler(AuthorizationException.class)
    public String handleAuthorizationException(AuthorizationException e, HttpServletRequest request) {
        // @RequiresPermissions / @RequiresRoles 校验不通过时 Shiro 抛的是
        // UnauthorizedException（AuthorizationException 的子类）。
        // 不加这个处理器的话它会掉进下面的 Exception 兜底分支，被记成
        // “未处理的服务器内部异常” + 500 + 把异常消息暴露给前端，
        // 明明是正常的“没权限”，日志里却全是 ERROR 堆栈，排查时极具误导性。
        log.warn("权限不足：{}", e.getMessage());

        request.setAttribute("javax.servlet.error.status_code", HttpStatus.FORBIDDEN.value());
        // MyErrorController.errorJson 用 business_status 当返回体里的 code，
        // 不设的话会兜底成 500，前端就分不清是“没权限”还是“服务端炸了”。
        request.setAttribute("business_status", HttpStatus.FORBIDDEN.value());
        request.setAttribute("javax.servlet.error.message", "没有权限执行该操作");
        request.setAttribute("javax.servlet.error.exception", e);

        String accept = request.getHeader("accept");
        if (accept == null) { accept = ""; }
        if (accept.contains("text/html,")){
            return "forward:/errorHtml";
        }else
        {
            return "forward:/errorJson";
        }
    }

    /**
     * 兜底处理所有未被上面方法捕获的其他异常（包括空指针、数组越界、数据库异常等）
     * 这类异常通常是服务器内部错误（bug），HTTP 状态码使用 500
     */
    @ExceptionHandler(Exception.class)
    public String handleGenericException(Exception e, HttpServletRequest request) {
        log.error("未处理的服务器内部异常", e);  // 打印完整堆栈，方便排查

        request.setAttribute("javax.servlet.error.status_code", HttpStatus.INTERNAL_SERVER_ERROR.value());
        // 生产环境中不建议把异常的具体消息直接暴露给用户，可以用更通用的提示
        request.setAttribute("javax.servlet.error.message", "服务器内部错误:"+e.getMessage());
        request.setAttribute("javax.servlet.error.exception", e);
        // 转发到 /error 端点，由 MyErrorController 接手
        String accept = request.getHeader("accept");
        if (accept == null) { accept = ""; } // 修复：无 Accept 头时原代码会 NPE
        if (accept.contains("text/html,")){
            return "forward:/errorHtml";
        }else
        {
            return "forward:/errorJson";
        }
    }
}
