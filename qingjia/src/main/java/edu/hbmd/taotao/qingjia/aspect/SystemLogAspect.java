package edu.hbmd.taotao.qingjia.aspect;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

@Component
@Aspect
public class SystemLogAspect {
    private static final Logger logger = LoggerFactory.getLogger(SystemLogAspect.class);

    // 定义切入点：监控项目中所有 public 方法
    @Pointcut("execution(public * edu.hbmd.taotao.qingjia..*.*(..))")
    public void allPublicMethods() {
    }

    // 定义切入点：监控 Service 层所有方法
    @Pointcut("execution(* edu.hbmd.taotao.qingjia..*Service.*(..))")
    public void serviceMethods() {
    }

    // 定义切入点：监控 Controller 层所有方法
    @Pointcut("execution(* edu.hbmd.taotao.qingjia..*Controller.*(..))")
    public void controllerMethods() {
    }

    /**
     * 环绕通知 - 记录方法执行的详细信息
     */
    @Around("allPublicMethods()")
    public Object logMethodInvocation(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String fullMethodName = className + "." + methodName;

        Object[] args = joinPoint.getArgs();
        String parameters = args != null && args.length > 0 ? Arrays.toString(args) : "无参数";

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String returnType = method.getReturnType().getSimpleName();

        long startTime = System.currentTimeMillis();
        String startTimeStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());

        logger.info("【方法开始】{} | 返回类型: {} | 参数: {} | 开始时间: {}",
                fullMethodName, returnType, parameters, startTimeStr);

        if (logger.isDebugEnabled()) {
            logger.debug("【方法详情】类: {} | 方法: {} | 参数类型: {}",
                    className, methodName, Arrays.toString(method.getParameterTypes()));
        }

        Object result = null;
        boolean success = false;

        try {
            result = joinPoint.proceed(args);
            success = true;

            if (logger.isDebugEnabled()) {
                String resultStr = result != null ? result.toString() : "null";
                if (resultStr.length() > 500) {
                    resultStr = resultStr.substring(0, 500) + "...(太长，已截断)";
                }
                logger.debug("【方法返回】{} | 返回值: {}", fullMethodName, resultStr);
            }
            return result;

        } catch (Throwable e) {
            success = false;
            logger.error("【方法异常】{} | 异常类型: {} | 异常消息: {}",
                    fullMethodName, e.getClass().getName(), e.getMessage(), e);
            throw e;

        } finally {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            String endTimeStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
            String status = success ? "成功" : "失败";
            String durationInfo = formatDuration(duration);

            logger.info("【方法结束】{} | 状态: {} | 执行时长: {} ({} ms) | 结束时间: {}",
                    fullMethodName, status, durationInfo, duration, endTimeStr);

            if (duration > 3000) {
                logger.warn("【性能告警】方法 {} 执行时间过长: {} ({} ms)",
                        fullMethodName, durationInfo, duration);
            }
        }
    }

    @Before("serviceMethods()")
    public void beforeServiceMethod(JoinPoint joinPoint) {
        if (logger.isDebugEnabled()) {
            String className = joinPoint.getTarget().getClass().getSimpleName();
            String methodName = joinPoint.getSignature().getName();
            Object[] args = joinPoint.getArgs();
            logger.debug("【Service调用前】{}.{} | 线程: {} | 参数: {}",
                    className, methodName, getThreadInfo(),
                    args != null ? Arrays.toString(args) : "无参数");
        }
    }

    @AfterReturning(pointcut = "serviceMethods()", returning = "result")
    public void afterServiceMethod(JoinPoint joinPoint, Object result) {
        if (logger.isDebugEnabled()) {
            String className = joinPoint.getTarget().getClass().getSimpleName();
            String methodName = joinPoint.getSignature().getName();
            String resultStr = result != null ? result.toString() : "null";
            if (resultStr.length() > 200) {
                resultStr = resultStr.substring(0, 200) + "...";
            }
            logger.debug("【Service调用后】{}.{} | 返回值: {}", className, methodName, resultStr);
        }
    }

    @AfterThrowing(pointcut = "serviceMethods()", throwing = "ex")
    public void afterServiceException(JoinPoint joinPoint, Exception ex) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        logger.error("【Service异常】{}.{} | 异常类型: {} | 异常消息: {}",
                className, methodName, ex.getClass().getName(), ex.getMessage(), ex);
    }

    private String formatDuration(long millis) {
        if (millis < 1000) {
            return millis + " ms";
        } else if (millis < 60000) {
            return String.format("%.2f 秒", millis / 1000.0);
        } else {
            return String.format("%d 分 %d 秒", millis / 60000, (millis % 60000) / 1000);
        }
    }

    private String getThreadInfo() {
        Thread thread = Thread.currentThread();
        return String.format("ID: %d, Name: %s", thread.getId(), thread.getName());
    }
}
