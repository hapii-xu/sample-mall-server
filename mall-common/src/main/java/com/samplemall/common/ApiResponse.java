package com.samplemall.common;

/**
 * 统一响应体：成功 {@code {"code":"OK","message":"ok","data":...}}，
 * 失败 {@code {"code":"<ERROR_CODE>","message":"<中文提示>","data":null}}。
 * 前端直接展示 message，错误码供自动化断言。
 */
public record ApiResponse<T>(String code, String message, T data) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>("OK", "ok", data);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>("OK", "ok", null);
    }

    public static ApiResponse<Void> error(String code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
