package com.faraday.flashsell.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.faraday.flashsell.model.entity.SeckillGoods;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SeckillGoodsMapper extends BaseMapper<SeckillGoods> {

    /**
     * 扣减秒杀商品库存（乐观锁版本）
     * WHERE stock > 0 + version = #{version} 双重防护
     * version = version + 1 保证并发安全
     *
     * @param seckillGoodsId 秒杀商品 ID
     * @param version        当前版本号
     * @return 影响行数（0 表示库存不足或版本冲突）
     */
    @Update("UPDATE seckill_goods SET stock = stock - 1, version = version + 1 " +
            "WHERE id = #{seckillGoodsId} AND stock > 0 AND version = #{version}")
    int reduceStock(@Param("seckillGoodsId") Long seckillGoodsId, @Param("version") Integer version);
}

