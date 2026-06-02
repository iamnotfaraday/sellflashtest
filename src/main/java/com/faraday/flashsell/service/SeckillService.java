package com.faraday.flashsell.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.faraday.flashsell.common.response.Result;
import com.faraday.flashsell.dao.OrderMapper;
import com.faraday.flashsell.dao.SeckillGoodsMapper;
import com.faraday.flashsell.model.dto.SeckillDTO;
import com.faraday.flashsell.model.entity.OrderInfo;
import com.faraday.flashsell.model.entity.SeckillGoods;
import com.faraday.flashsell.model.vo.SeckillResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class SeckillService {

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Autowired
    private OrderMapper orderMapper;

    /**
     * V1 裸奔版 — 直接 MySQL 扣库存
     *
     * ══════════ 完整执行流程 ══════════
     *
     * ① 查秒杀商品
     *    seckillGoodsMapper.selectById(goodsId)
     *    查不到 → 返回 "商品不存在"
     *
     * ② 校验活动时间
     *    LocalDateTime.now() 是否在 start_time ~ end_time 之间
     *    未开始 → 返回 "活动未开始"
     *    已结束 → 返回 "活动已结束"
     *
     * ③ 校验库存
     *    goods.getStock() <= 0
     *    没货了 → 返回 "库存不足"
     *
     * ④ 防重复秒杀
     *    查 order_info 表：user_id = ? AND goods_id = ?
     *    已有订单 → 返回 "你已经抢过了"
     *    【建议】建表时给 (user_id, goods_id) 加唯一索引兜底
     *
     * ⑤ 扣库存（★ V1 致命问题在这里 ★）
     *    UPDATE seckill_goods SET stock = stock - 1 WHERE id = ? AND stock > 0
     *    affectedRows == 0 → 被别人抢光了 → 返回 "库存不足"
     *
     *    为什么超卖？
     *    线程A读到 stock=5，线程B也读到 stock=5
     *    两个都执行 stock-1 → 5 件库存可能卖出 10 单
     *
     * ⑥ 创建订单
     *    orderNumber = UUID.randomUUID().toString()
     *    INSERT INTO order_info (user_id, goods_id, seckill_goods_id, order_number, status)
     *    VALUES (?, ?, ?, ?, 0)
     *
     * ⑦ 返回
     *    Result.success(new SeckillResultVO(orderNumber, 0))
     *
     *
     * ══════════ V1 存在的问题 ══════════
     * 1. 超卖：库存扣减不是原子的
     * 2. 性能：MySQL 扛不住高并发读写
     * 3. 无排队：1000 个请求同时打 DB，连接池瞬间满
     */
    @Transactional
    public Result<SeckillResultVO> execute(SeckillDTO dto, Long userId) {

        // ─── 第一步：查秒杀商品 ───
        SeckillGoods goods = seckillGoodsMapper.selectById(dto.getGoodsId());
        if (goods == null) {
            return Result.fail(50001, "商品不存在");
        }

        // ─── 第二步：校验活动时间 ───
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(goods.getStartTime())) {
            return Result.fail(50002, "活动未开始");
        }
        if (now.isAfter(goods.getEndTime())) {
            System.out.println("活动已结束");
            return Result.fail(50003, "活动已结束");
        }

        // ─── 第三步：校验库存 ───
        if (goods.getStock() <= 0) {
            return Result.fail(50004, "库存不足");
        }

        // ─── 第四步：防重复秒杀 ───
        // TODO: 你需要自己实现，思路如下：
        //   Long userId = ???;  // 怎么拿到当前用户ID？两种办法：
        //     ① 临时：SeckillDTO 里加个 userId 字段，前端传过来
        //     ② 正式：从 Authorization 头解析 JWT 拿到 userId
        //
        OrderInfo existOrder = orderMapper.selectOne(
            new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getUserId, userId)
                .eq(OrderInfo::getGoodsId, dto.getGoodsId())
        );
        if (existOrder != null) {
            return Result.fail(50005, "你已经抢过了");
        }

        // ─── 第五步：扣库存 ───
        int affectedRows = seckillGoodsMapper.reduceStock(dto.getGoodsId());
        // TODO: 在 SeckillGoodsMapper 里加一个方法：
        //   @Update("UPDATE seckill_goods SET stock = stock - 1 WHERE id = #{id} AND stock > 0")
        //   int reduceStock(@Param("id") Long id);

        if (affectedRows == 0) {
            return Result.fail(50004, "库存不足");
        }

        // ─── 第六步：创建订单 ───
        OrderInfo order = new OrderInfo();
        order.setUserId(userId);
        order.setGoodsId(goods.getGoodsId());
        order.setSeckillGoodsId(goods.getId());
//        order.setGoodsName(goods.getGoodsName());
        order.setGoodsPrice(goods.getSeckillPrice());
        order.setOrderNumber(UUID.randomUUID().toString());
        order.setStatus(0);
        orderMapper.insert(order);

        // ─── 第七步：返回 ───
        return Result.success(new SeckillResultVO(order.getOrderNumber(), 0));
    }
}