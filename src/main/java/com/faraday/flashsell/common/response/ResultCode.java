package com.faraday.flashsell.common.response;

public enum ResultCode {

    SUCCESS(200, "操作成功"),
    LOGIN_FAIL(401, "手机号或密码错误"),
    ERROR(500, "服务器异常"),

    // 秒杀相关
    GOODS_NOT_FOUND(50001, "商品不存在"),
    ACTIVITY_NOT_STARTED(50002, "活动未开始"),
    ACTIVITY_ENDED(50003, "活动已结束"),
    SEC_KILL_NO_STOCK(50004, "库存不足"),
    SEC_KILL_REPEAT(50005, "你已经抢过了"),
    NO_SEC_KILL_ACTIVITY(50006, "该商品暂无秒杀活动");

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
