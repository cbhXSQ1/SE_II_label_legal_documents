package com.seii.legal.common.result;

/**
 * 认证或权限异常：登录凭证失效、账号停用、无权限访问时抛出。
 */
public class AuthException extends RuntimeException {

    public AuthException(String message) {
        super(message);
    }
}
