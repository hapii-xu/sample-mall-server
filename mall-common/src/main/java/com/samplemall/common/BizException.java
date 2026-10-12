package com.samplemall.common;

import org.springframework.http.HttpStatus;

/** 业务异常：携带 HTTP 状态、错误码与面向用户的中文提示。 */
public class BizException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public BizException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static BizException badRequest(String code, String message) {
        return new BizException(HttpStatus.BAD_REQUEST, code, message);
    }

    public static BizException unauthorized(String code, String message) {
        return new BizException(HttpStatus.UNAUTHORIZED, code, message);
    }

    public static BizException notFound(String code, String message) {
        return new BizException(HttpStatus.NOT_FOUND, code, message);
    }

    public static BizException conflict(String code, String message) {
        return new BizException(HttpStatus.CONFLICT, code, message);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
