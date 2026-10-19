package edu.hbmd.taotao.qingjia.exception;

public class ResultExceptionSuccess extends ResultException {
    /**
     * 统一异常处理：抛出默认成功信息
     */
    public ResultExceptionSuccess() {
        super(ResultEnum.SUCCESS);
    }

    /**
     * 统一异常处理：抛出成功提示信息
     * @param message 提示信息
     */
    public ResultExceptionSuccess(String message) {
        super(ResultEnum.SUCCESS.getStatus(), message);
    }
}
