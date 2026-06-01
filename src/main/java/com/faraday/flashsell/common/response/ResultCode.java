package com.faraday.flashsell.common.response;

public enum ResultCode {

    SUCCESS(200, "操作成功"),
    LOGIN_FAIL(401, "手机号或密码错误"),
    ERROR(500, "服务器异常");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
