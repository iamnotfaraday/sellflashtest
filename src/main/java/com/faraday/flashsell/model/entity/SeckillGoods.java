package com.faraday.flashsell.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("seckill_goods")
public class SeckillGoods {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long goodsId;

    /** 秒杀价格 */
    private BigDecimal seckillPrice;

    /** 商品名称（冗余，避免联表） */
    private String goodsName;

    /** 秒杀库存 */
    private Integer stock;

    /** 秒杀开始时间 */
    private LocalDateTime startTime;

    /** 秒杀结束时间 */
    private LocalDateTime endTime;
}
