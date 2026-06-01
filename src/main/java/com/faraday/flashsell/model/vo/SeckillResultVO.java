package com.faraday.flashsell.model.vo;

import lombok.Data;

/**
 * 秒杀结果，返回给前端
 */
@Data
public class SeckillResultVO {

    /** 订单编号 */
    private String orderNumber;

    /** 订单状态：0-未支付 1-已支付 */
    private Integer status;

    public SeckillResultVO(String orderNumber, Integer status) {
        this.orderNumber = orderNumber;
        this.status = status;
    }
}