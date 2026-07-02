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

    /** 乐观锁最大重试次数 */
    private static final int MAX_RETRY = 3;

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

        // ─── 第三步：快速库存检查 ───
        if (goods.getStock() <= 0) {
            return Result.fail(ResultCode.SEC_KILL_NO_STOCK);
        }

        // ─── 第四步：防重复秒杀 ───
        // 说明：此处是快照读，并发下不一定准确，真正的兜底靠 order_info 表的
        //       UNIQUE KEY (user_id, goods_id) 约束（见第七步的 catch）。
        OrderInfo existOrder = orderMapper.selectOne(
            new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getUserId, userId)
                .eq(OrderInfo::getGoodsId, goods.getGoodsId())
        );
        if (existOrder != null) {
            return Result.fail(ResultCode.SEC_KILL_REPEAT);
        }

        // ─── 第五步：乐观锁扣库存（重试机制） ───
        // 每次 UPDATE 携带当前 version，WHERE version = #{version} 保证互斥
        // 冲突时：若库存还有则重试（最多3次），库存真没了则返回售罄
        int affectedRows = 0;
        Integer currentVersion = goods.getVersion();

        for (int retry = 0; retry < MAX_RETRY; retry++) {
            affectedRows = seckillGoodsMapper.reduceStock(goods.getId(), currentVersion);
            if (affectedRows > 0) {
                break; // 扣库存成功
            }

            // 失败 → 重新查询，区分"售罄"和"版本冲突"
            SeckillGoods latest = seckillGoodsMapper.selectById(goods.getId());
            if (latest.getStock() <= 0) {
                log.info("goodsId={} 库存已售罄，放弃重试", dto.getGoodsId());
                return Result.fail(ResultCode.SEC_KILL_NO_STOCK);
            }

            // 库存还在说明是版本冲突，换新版本重试
            currentVersion = latest.getVersion();
            log.warn("乐观锁冲突重试：userId={} goodsId={} retry={}/{} curVersion={}",
                    userId, dto.getGoodsId(), retry + 1, MAX_RETRY, currentVersion);
        }

        if (affectedRows == 0) {
            log.warn("goodsId={} 乐观锁重试{}次仍失败", dto.getGoodsId(), MAX_RETRY);
            return Result.fail(ResultCode.SEC_KILL_NO_STOCK);
        }

        // ─── 第六步：创建订单 ───
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
            // ─── 第七步：唯一约束冲突处理 ───
            // 触发的场景：
            //   第四步的 SELECT 因为并发读到了过期快照，没拦住重复请求
            //   导致第五步多扣了库存，第六步的 INSERT 被唯一约束挡住
            //
            // 处理方式：标记事务回滚 → 第五步扣掉的库存被自动归还（MySQL 回滚）
            // 用户最终看到 "你已经抢过了"
            log.warn("重复秒杀拦截：userId={}, goodsId={}", userId, goods.getGoodsId());
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return Result.fail(ResultCode.SEC_KILL_REPEAT);
        }

        // ─── 第八步：返回 ───
        return Result.success(new SeckillResultVO(String.valueOf(order.getId()), 0));
    }
}