```
flashsell/
│
├── pom.xml                                          # Maven 依赖管理
├── README.md                                        # 项目说明 & 演进路线
│
└── src/
    ├── main/
    │   ├── java/com/faraday/flashsell/
    │   │   │
    │   │   ├── FlashsellApplication.java            # Spring Boot 启动类
    │   │   │
    │   │   ├── config/                               # ═══ 配置层 ═══
    │   │   │   ├── WebMvcConfig.java                 # MVC 配置（拦截器注册、CORS）
    │   │   │   ├── RedisConfig.java                  # Redis 序列化 & 连接池配置
    │   │   │   ├── ThreadPoolConfig.java             # 线程池配置（异步下单用）
    │   │   │   ├── RedissonConfig.java               # Redisson 分布式锁配置（V3+）
    │   │   │   ├── RabbitMQConfig.java               # 消息队列配置（V5+）
    │   │   │   └── RateLimiterConfig.java            # 限流器配置（V6+）
    │   │   │
    │   │   ├── controller/                           # ═══ 控制器层 ═══
    │   │   │   ├── UserController.java               # 用户注册/登录/登出
    │   │   │   ├── GoodsController.java              # 商品列表/商品详情
    │   │   │   ├── SeckillController.java            # ★ 核心：秒杀按钮接口
    │   │   │   └── OrderController.java              # 订单查询（我的秒杀结果）
    │   │   │
    │   │   ├── service/                              # ═══ 业务服务层 ═══
    │   │   │   ├── UserService.java                  # 用户服务接口
    │   │   │   ├── GoodsService.java                 # 商品服务接口
    │   │   │   ├── OrderService.java                 # 订单服务接口
    │   │   │   │
    │   │   │   ├── seckill/                          # ★ 秒杀服务（策略模式，可切换版本）
    │   │   │   │   ├── SeckillService.java           #   秒杀接口定义
    │   │   │   │   ├── v1/                           #   V1：裸奔版（直接 MySQL 扣库存）
    │   │   │   │   │   └── SeckillServiceV1Impl.java
    │   │   │   │   ├── v2/                           #   V2：乐观锁版（MySQL version 字段）
    │   │   │   │   │   └── SeckillServiceV2Impl.java
    │   │   │   │   ├── v3/                           #   V3：Redis 预扣库存 + 异步落库
    │   │   │   │   │   └── SeckillServiceV3Impl.java
    │   │   │   │   ├── v4/                           #   V4：Redis + Lua 原子脚本
    │   │   │   │   │   ├── SeckillServiceV4Impl.java
    │   │   │   │   │   └── seckill.lua               #   Lua 脚本文件
    │   │   │   │   ├── v5/                           #   V5：MQ 削峰（RabbitMQ）
    │   │   │   │   │   ├── SeckillServiceV5Impl.java
    │   │   │   │   │   ├── SeckillMessageProducer.java
    │   │   │   │   │   └── SeckillMessageConsumer.java
    │   │   │   │   └── v6/                           #   V6：限流防刷（令牌桶 + 验证码）
    │   │   │   │       └── SeckillServiceV6Impl.java
    │   │   │   │
    │   │   │   └── impl/                             # 常规服务实现
    │   │   │       ├── UserServiceImpl.java
    │   │   │       ├── GoodsServiceImpl.java
    │   │   │       └── OrderServiceImpl.java
    │   │   │
    │   │   ├── dao/                                  # ═══ 数据访问层 ═══
    │   │   │   ├── UserMapper.java                   # MyBatis Mapper（或用 JPA Repository）
    │   │   │   ├── GoodsMapper.java
    │   │   │   ├── OrderMapper.java
    │   │   │   └── SeckillGoodsMapper.java           # 秒杀商品（含秒杀库存 + 版本号）
    │   │   │
    │   │   ├── model/                                # ═══ 数据模型层 ═══
    │   │   │   ├── entity/                           # 数据库实体
    │   │   │   │   ├── User.java                     #   id, nickname, phone, password, salt
    │   │   │   │   ├── Goods.java                    #   商品基础信息
    │   │   │   │   ├── SeckillGoods.java             #   秒杀商品（goods_id, seckill_price,
    │   │   │   │   │                                  #   stock, version, start_time, end_time）
    │   │   │   │   └── OrderInfo.java                #   订单（user_id, goods_id, status, create_time）
    │   │   │   │
    │   │   │   ├── dto/                              # 数据传输对象（接口入参）
    │   │   │   │   ├── LoginDTO.java                 #   登录请求 {phone, password}
    │   │   │   │   ├── RegisterDTO.java              #   注册请求
    │   │   │   │   └── SeckillDTO.java               #   秒杀请求 {goodsId, userId, verifyCode}
    │   │   │   │
    │   │   │   └── vo/                               # 视图对象（接口出参）
    │   │   │       ├── UserVO.java                   #   用户信息（脱敏）
    │   │   │       ├── GoodsVO.java                  #   商品详情（含秒杀状态倒计时）
    │   │   │       ├── SeckillResultVO.java          #   秒杀结果 {orderId, status}
    │   │   │       └── OrderVO.java                  #   订单详情
    │   │   │
    │   │   ├── common/                               # ═══ 公共模块 ═══
    │   │   │   ├── response/                         # 统一响应封装
    │   │   │   │   ├── Result.java                   #   统一返回体 {code, message, data}
    │   │   │   │   └── ResultCode.java               #   状态码枚举
    │   │   │   │
    │   │   │   ├── exception/                        # 异常处理
    │   │   │   │   ├── GlobalExceptionHandler.java   #   @RestControllerAdvice 全局捕获
    │   │   │   │   ├── BusinessException.java        #   业务异常
    │   │   │   │   ├── SeckillException.java         #   秒杀异常（库存不足/重复秒杀等）
    │   │   │   │   └── AuthException.java            #   认证异常
    │   │   │   │
    │   │   │   ├── utils/                            # 工具类
    │   │   │   │   ├── MD5Utils.java                 #   密码加盐 MD5
    │   │   │   │   ├── JwtUtils.java                 #   JWT 生成 & 校验
    │   │   │   │   ├── CookieUtils.java              #   Cookie 读写
    │   │   │   │   ├── UUIDUtils.java                #   唯一 ID 生成
    │   │   │   │   └── RedisKeyUtils.java            #   Redis Key 统一前缀管理
    │   │   │   │
    │   │   │   └── constant/                         # 常量
    │   │   │       ├── RedisKeyPrefix.java           #   Redis Key 前缀常量
    │   │   │       ├── SeckillStatus.java            #   秒杀状态枚举
    │   │   │       └── OrderStatus.java              #   订单状态枚举
    │   │   │
    │   │   ├── interceptor/                          # ═══ 拦截器层 ═══
    │   │   │   └── LoginInterceptor.java             # 登录拦截器（校验 JWT/Cookie）
    │   │   │
    │   │   └── security/                             # ═══ 安全模块 ═══
    │   │       ├── UserContext.java                  # ThreadLocal 保存当前登录用户
    │   │       └── AccessLimit.java                  # 自定义注解（接口限流，V6）
    │   │
    │   └── resources/
    │       ├── application.properties                # 基础配置
    │       ├── application-dev.yml                   # 开发环境配置
    │       ├── application-prod.yml                  # 生产环境配置
    │       ├── static/                               # 静态资源
    │       │   ├── index.html                        #   秒杀首页（商品列表 + 秒杀按钮）
    │       │   ├── login.html                        #   登录注册页
    │       │   ├── css/
    │       │   │   └── style.css
    │       │   └── js/
    │       │       ├── common.js                     #   公共请求封装
    │       │       ├── login.js                      #   登录逻辑
    │       │       ├── goods.js                      #   商品列表 & 倒计时
    │       │       ├── seckill.js                    #   ★ 秒杀按钮核心交互
    │       │       └── order.js                      #   订单查询
    │       └── templates/                            # Thymeleaf 模板（可选，SSR 方案）
    │
    └── test/java/com/faraday/flashsell/
        ├── FlashsellApplicationTests.java
        ├── service/
        │   └── seckill/
        │       ├── SeckillServiceV1Test.java         # V1 单元测试
        │       ├── SeckillServiceV2Test.java         # V2 乐观锁测试
        │       └── SeckillServiceV4Test.java         # V4 Lua 脚本测试
        └── benchmark/                                # ═══ 压测脚本 ═══
            └── jmeter/
                ├── seckill-v1.jmx                    # JMeter 压测脚本 V1
                ├── seckill-v2.jmx                    # JMeter 压测脚本 V2
                ├── seckill-v4.jmx                    # JMeter 压测脚本 V4
                └── seckill-v6.jmx                    # JMeter 压测脚本 V6
```