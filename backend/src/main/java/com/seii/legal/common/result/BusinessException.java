package com.seii.legal.common.result;

/**
 * 业务异常：数据层、规则层或功能层校验不通过时抛出，由全局异常处理器转为 AppResult.fail。
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
