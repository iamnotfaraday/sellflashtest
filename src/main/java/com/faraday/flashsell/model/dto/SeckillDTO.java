package com.faraday.flashsell.model.dto;

import lombok.Data;

/**
 * 秒杀请求参数
 * 前端发送：{ "goodsId": 1 }
 */
@Data
public class SeckillDTO {

    /** 要秒杀的商品 ID */
    private Long goodsId;
}