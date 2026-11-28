package com.seii.legal.function.support;

import com.seii.legal.common.exception.AuthException;
import com.seii.legal.common.exception.BusinessException;
import com.seii.legal.common.result.AppResult;
import com.seii.legal.data.repository.DataConflictException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常适配器：将鉴权/业务/数据冲突/参数异常统一封装为 AppResult.fail，兜底不泄露内部堆栈。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthException.class)
    public AppResult<Void> handleAuth(AuthException ex) {
        return AppResult.fail(ex.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    public AppResult<Void> handleBusiness(BusinessException ex) {
        return AppResult.fail(ex.getMessage());
    }

    @ExceptionHandler(DataConflictException.class)
    public AppResult<Void> handleDataConflict(DataConflictException ex) {
        return AppResult.fail(ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public AppResult<Void> handleIllegalState(IllegalStateException ex) {
        return AppResult.fail(ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public AppResult<Void> handleIllegalArgument(IllegalArgumentException ex) {
        return AppResult.fail(ex.getMessage());
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public AppResult<Void> handleMissingHeader(MissingRequestHeaderException ex) {
        if ("Authorization".equalsIgnoreCase(ex.getHeaderName())) {
            return AppResult.fail("登录已失效，请重新登录");
        }
        return AppResult.fail("缺少请求头：" + ex.getHeaderName());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public AppResult<Void> handleMissingParam(MissingServletRequestParameterException ex) {
        return AppResult.fail("缺少必填参数：" + ex.getParameterName());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public AppResult<Void> handleUnreadable(HttpMessageNotReadableException ex) {
        return AppResult.fail("请求体格式错误");
    }

    @ExceptionHandler(Exception.class)
    public AppResult<Void> handleUnexpected(Exception ex) {
        return AppResult.fail("系统繁忙，请稍后重试");
    }
}
