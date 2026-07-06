# FlashSell — 高并发秒杀系统（渐进式演进）

一个渐进式的高并发秒杀（Seckill）项目，从 **V1 裸奔 MySQL** 开始，逐版本解决并发问题，最终演进为一个成熟的秒杀系统。

---

## V1 — 裸奔 MySQL（已完成）

### 架构

```
前端 → Controller → JWT 拦截器 → Service → MySQL
```
- MySQL InnoDB 行锁 + `stock > 0` 条件防止超卖
- `order_info` 表 `(user_id, goods_id)` 唯一约束防止重复下单
- `LoginInterceptor` 统一解析 JWT，Controller 无需关注 token 处理

### 执行流程

```
① 按商品ID查秒杀活动
② 校验活动时间窗口
③ 防重复秒杀（SELECT 快速路径 + 唯一约束兜底）
④ 扣库存（UPDATE ... SET stock = stock - 1 WHERE stock > 0）
⑤ 创建订单
⑥ 返回订单ID
```

### 已修复的问题

| 问题 | 影响 | 修复方式 |
|------|------|---------|
| 按主键查秒杀活动 | `selectById` 查不到记录 | 改为按 `goods_id` 字段查询 |
| 订单去重查错列 | 快读路径形同虚设 | 改为 `goods.getGoodsId()` |
| 库存预检查基于快照 | REPEATABLE READ 下读旧值 | 删除不可靠预检查，统一由 `reduceStock` 兜底 |
| UUID 订单号索引碎片 | B+ 树页分裂，插入性能下降 | 改用自增 ID 做订单号 |
| JWT 解析散落 Controller | 每个接口重复代码 | 抽取 `LoginInterceptor` 统一处理 |
| Magic Number 错误码 | 难以维护 | 改用 `ResultCode` 枚举 |
| CORS 预检被拦截 | 前端 OPTIONS 请求返回 401 | 拦截器放行 OPTIONS |

### 遗留问题（V2 解决）

- `@Transactional` 包裹只读查询
- `setRollbackOnly()` 偶发 500
- 依赖服务器时钟
- 无入参校验
- MySQL 行锁串行化，高并发 TPS 受限

### 压测结果

> TODO：插入 JMeter 压测截图

| 场景 | 线程数 | TPS | 成功率 | 说明 |
|------|--------|-----|--------|------|
| V1 直接扣库存 | 100 | — | — | |

---

## 后续演进

| 版本 | 方案 | 解决的问题 |
|------|------|-----------|
| **V2** | MySQL 乐观锁（version 字段） | 扣库存的竞态条件 |
| V3 | Redis 预扣库存 + 异步落库 | MySQL 写瓶颈 |
| V4 | Redis + Lua 原子脚本 | 库存扣减的原子性 |
| V5 | RabbitMQ 请求队列削峰 | 流量整形 |
| V6 | 令牌桶限流 + 验证码 | 流量控制 |

---

## 技术栈

| 技术 | 版本 |
|------|------|
| Java | 17 |
| Spring Boot | 3.2.5 |
| MyBatis-Plus | 3.5.9 |
| MySQL | 8.x |
| Redis | 待接入 |
| JWT (jjwt) | 0.12.6 |
| Lombok | — |

## 本地运行

```bash
# 编译
./mvnw clean package -DskipTests

# 启动（需要本地 MySQL，配置见 application.yml）
./mvnw spring-boot:run
```

## 压测

> TODO：JMeter 测试计划说明和 Token 生成方式
