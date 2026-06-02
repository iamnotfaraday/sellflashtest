package com.faraday.flashsell.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.faraday.flashsell.model.entity.SeckillGoods;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SeckillGoodsMapper extends BaseMapper<SeckillGoods> {

    /**
     * 扣减秒杀商品库存
     * WHERE stock > 0 防止超卖
     *
     * @param seckillGoodsId 秒杀商品 ID
     * @return 影响行数（0 表示库存不足）
     */
    @Update("UPDATE seckill_goods SET stock = stock - 1 WHERE id = #{seckillGoodsId} AND stock > 0")
    int reduceStock(@Param("seckillGoodsId") Long seckillGoodsId);
}
