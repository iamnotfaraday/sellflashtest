package com.faraday.flashsell.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("order_info")
public class OrderInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long goodsId;

    private Long seckillGoodsId;

    /** 订单编号 */
    private String orderNumber;

    /** 商品名称快照 */
    private String goodsName;

    /** 秒杀成交价 */
    private BigDecimal goodsPrice;

    /** 订单状态：0-未支付 1-已支付 2-已取消 3-已退款 */
    private Integer status;
}
