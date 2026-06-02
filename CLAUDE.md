# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
# Build and run tests
./mvnw clean package

# Skip tests
./mvnw clean package -DskipTests

# Run the app
./mvnw spring-boot:run

# Run a single test class
./mvnw test -Dtest=FlashsellApplicationTests
```

## Tech Stack

- **Java 17**, **Spring Boot 3.2.5**, **Maven** (mvnw wrapper)
- **MyBatis-Plus 3.5.9** (ORM with `BaseMapper`, `LambdaQueryWrapper`)
- **MySQL** (`flashsell` database on localhost:3306, root/123456)
- **Redis** (dependency present but not yet wired in code)
- **JWT** (jjwt 0.12.6) for auth — `JwtUtils` generates/parses tokens; controller extracts `Authorization: Bearer <token>` header
- **Lombok** — `@Data` on entities/DTOs/VOs

## Architecture (Current State)

This is a **progressive flash-sale (seckill) tutorial project**. Only **V1** is implemented so far — a bare-metal MySQL stock deduction with known race conditions. The project evolves through 6 versions, each fixing the previous version's concurrency problem.

### Current implemented files

| Layer | Files | Role |
|---|---|---|
| Entry | `FlashsellApplication.java` | `@SpringBootApplication` |
| Config | `CorsConfig.java` | CORS for `/api/**` |
| Controller | `UserController.java` | `POST /api/user/login` → `LoginVO` |
| Controller | `SeckillController.java` | `POST /api/seckill/execute` — extracts JWT userId, delegates to `SeckillService` |
| Service | `UserService.java` | Login: query by phone, MD5 password check, issue JWT |
| Service | `SeckillService.java` | V1 seckill — single `@Transactional` method (see flow below) |
| DAO | `UserMapper.java` | `BaseMapper<User>` |
| DAO | `SeckillGoodsMapper.java` | `BaseMapper<SeckillGoods>` + custom `reduceStock` (`UPDATE … SET stock = stock - 1 WHERE id = ? AND stock > 0`) |
| DAO | `OrderMapper.java` | `BaseMapper<OrderInfo>` |
| Model | `User`, `SeckillGoods`, `OrderInfo` | MyBatis-Plus entities |
| DTO | `LoginDTO`, `SeckillDTO` | Request bodies |
| VO | `LoginVO`, `SeckillResultVO` | Response bodies |
| Common | `Result<T>`, `ResultCode` | Unified response wrapper `{code, message, data}` |
| Common | `MD5Utils`, `JwtUtils` | Auth utilities |

### V1 Seckill Flow (in `SeckillService.execute`)

1. Query `SeckillGoods` by `goodsId`
2. Validate activity time window (`startTime` ≤ now ≤ `endTime`)
3. Check `stock > 0`
4. Deduplicate: check `order_info` for existing `(userId, goodsId)` pair
5. **Reduce stock**: `UPDATE seckill_goods SET stock = stock - 1 WHERE id = ? AND stock > 0` — **race condition here**, multiple threads can both see `stock > 0` and both deduct
6. Insert `OrderInfo` with random UUID order number
7. Return `SeckillResultVO`

### Planned Evolution (from `structure.md`, not yet coded)

- **V2**: Optimistic locking with MySQL `version` field
- **V3**: Redis pre-deduct inventory + async MySQL persistence
- **V4**: Redis + Lua atomic script for stock deduction
- **V5**: RabbitMQ for request queuing/peak-shaving
- **V6**: Rate limiting (token bucket) + CAPTCHA

### Key Patterns

- **Result wrapper**: All API responses use `Result<T>` with `Result.success(data)` / `Result.fail(code, message)`. Error codes defined in `ResultCode` enum.
- **JWT auth**: Controllers extract `Authorization` header, strip `Bearer ` prefix, parse with `JwtUtils.getUserId(token)`. No interceptor yet — auth is done inline in each controller method.
- **MyBatis-Plus**: Entities use `@TableId(type = IdType.AUTO)`. DAOs extend `BaseMapper<T>` — CRUD methods are inherited, no XML mappers needed. Custom SQL uses annotations (`@Update`, `@Select`).
- **No global exception handler yet** — errors are returned as `Result.fail()` rather than thrown.

### Test Files (under `src/test`)

- `FlashsellApplicationTests.java` — context load smoke test
- `TokenGenerator.java` — bulk-generates JWT tokens for JMeter load testing (writes to `D:/tokens.txt`)
- `api_test.jmx` — currently empty (0 bytes), intended JMeter test plan

### Configuration

Single `application.yml`: port 8080, MySQL datasource, MyBatis-Plus camelCase mapping + SQL logging enabled, JWT secret + 7-day expiration. No environment profiles configured yet.
