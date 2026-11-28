package com.seii.legal.common.result;

import java.util.List;

/**
 * 统一 HTTP 响应体：前端统一解包 success/message/data/allowedActions。
 */
public record AppResult<T>(boolean success, String message, T data, List<String> allowedActions) {

    public static <T> AppResult<T> ok(T data) {
        return new AppResult<>(true, "成功", data, List.of());
    }

    public static <T> AppResult<T> ok(T data, List<String> allowedActions) {
        return new AppResult<>(true, "成功", data, allowedActions == null ? List.of() : allowedActions);
    }

    public static <T> AppResult<T> fail(String message) {
        return new AppResult<>(false, message, null, List.of());
    }
}
