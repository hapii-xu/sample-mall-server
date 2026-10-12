package com.samplemall.common;

/** 网关校验令牌后注入下游请求的用户身份头（下游服务只信任网关）。 */
public final class UserHeaders {

    public static final String USER_ID = "X-User-Id";
    public static final String USERNAME = "X-User-Name";

    private UserHeaders() {
    }
}
