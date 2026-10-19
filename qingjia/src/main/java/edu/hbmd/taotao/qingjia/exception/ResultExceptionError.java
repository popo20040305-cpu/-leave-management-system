package edu.hbmd.taotao.qingjia.exception;

public class ResultExceptionError extends  ResultException  {
    /**
     * 统一异常处理：抛出默认失败信息
     */
    public ResultExceptionError() {
        super(ResultEnum.ERROR);
    }

    /**
     * 统一异常处理：抛出失败提示信息
     * @param message 提示信息
     */
    public ResultExceptionError(String message) {
        super(ResultEnum.ERROR.getStatus(), message);
    }
    /**
     * 统一异常处理：抛出失败提示信息
     * @param message 提示信息
     */
    public ResultExceptionError(String message,Throwable e) {
        super(ResultEnum.ERROR.getStatus(), message,e);
    }

}
