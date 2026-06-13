package com.faraday.flashsell.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.faraday.flashsell.common.response.Result;
import com.faraday.flashsell.common.response.ResultCode;
import com.faraday.flashsell.dao.OrderMapper;
import com.faraday.flashsell.dao.SeckillGoodsMapper;
import com.faraday.flashsell.model.dto.SeckillDTO;
import com.faraday.flashsell.model.entity.OrderInfo;
import com.faraday.flashsell.model.entity.SeckillGoods;
import com.faraday.flashsell.model.vo.SeckillResultVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class SeckillService {

    private static final Logger log = LoggerFactory.getLogger(SeckillService.class);

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Transactional
    public Result<SeckillResultVO> execute(SeckillDTO dto, Long userId) {

        // ─── 第一步：查秒杀商品（按原始商品ID查秒杀活动） ───
        SeckillGoods goods = seckillGoodsMapper.selectOne(
            new LambdaQueryWrapper<SeckillGoods>()
                .eq(SeckillGoods::getGoodsId, dto.getGoodsId())
        );
        if (goods == null) {
            return Result.fail(ResultCode.NO_SEC_KILL_ACTIVITY);
        }

        // ─── 第二步：校验活动时间 ───
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(goods.getStartTime())) {
            return Result.fail(ResultCode.ACTIVITY_NOT_STARTED);
        }
        if (now.isAfter(goods.getEndTime())) {
            log.warn("goodsId={} 活动已结束", dto.getGoodsId());
            return Result.fail(ResultCode.ACTIVITY_ENDED);
        }

        // ─── 第三步：防重复秒杀（快速路径） ───
        // 说明：此处是快照读，并发下不一定准确，真正的兜底靠 order_info 表的
        //       UNIQUE KEY (user_id, goods_id) 约束（见第六步的 catch）。
        //       这个 SELECT 的作用只是提前拦截大部分重复请求，减少不必要的扣库存。
        OrderInfo existOrder = orderMapper.selectOne(
            new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getUserId, userId)
                .eq(OrderInfo::getGoodsId, goods.getGoodsId())
        );
        if (existOrder != null) {
            return Result.fail(ResultCode.SEC_KILL_REPEAT);
        }

        // ─── 第四步：扣库存（原子操作） ───
        // WHERE stock > 0 是真正的库存防线，防止超卖
        int affectedRows = seckillGoodsMapper.reduceStock(goods.getId());
        if (affectedRows == 0) {
            return Result.fail(ResultCode.SEC_KILL_NO_STOCK);
        }

        // ─── 第五步：创建订单 ───
        OrderInfo order = new OrderInfo();
        order.setUserId(userId);
        order.setGoodsId(goods.getGoodsId());
        order.setSeckillGoodsId(goods.getId());
        order.setGoodsName(goods.getGoodsName());
        order.setGoodsPrice(goods.getSeckillPrice());
        order.setOrderNumber(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")) + userId);
        order.setStatus(0);

        try {
            orderMapper.insert(order);
        } catch (DuplicateKeyException e) {
            // ─── 第六步：唯一约束冲突处理 ───
            // 触发的场景：
            //   第三步的 SELECT 因为并发读到了过期快照，没拦住重复请求
            //   导致第四步多扣了库存，第五步的 INSERT 被唯一约束挡住
            //
            // 处理方式：标记事务回滚 → 第四步扣掉的库存被自动归还
            // 用户最终看到 "你已经抢过了"
            log.warn("重复秒杀拦截：userId={}, goodsId={}", userId, goods.getGoodsId());
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return Result.fail(ResultCode.SEC_KILL_REPEAT);
        }

        // ─── 第七步：返回 ───
        return Result.success(new SeckillResultVO(String.valueOf(order.getId()), 0));
    }
}