# sample-mall-server

Sample Mall（示例商城 · 山野茶铺）后端：Spring Boot 4 + Spring Cloud Gateway 微服务。
前端见 [sample-mall-web](https://github.com/hapii-xu/sample-mall-web)，需求文档见 [sample-mall-docs](https://github.com/hapii-xu/sample-mall-docs)。

> 定位：**演示 / 测试用业务系统**，用于端到端验证 Tapestry 平台（需求、迭代、用例、自动化、知识库、代码集成）。
> 数据确定、每次启动重建，适合自动化断言。

## 架构

```
浏览器 ──/api──▶ mall-gateway :8080 ──┬─▶ mall-user    :8081  登录、个人信息
          (JWT 校验，注入 X-User-Id)   ├─▶ mall-product :8082  分类、商品、库存
                                       └─▶ mall-order   :8083  购物车、下单、支付、订单
                                               └──/internal──▶ mall-product（取快照 / 扣减 / 回补库存）
```

| 模块 | 说明 |
|---|---|
| `mall-common` | 统一响应体 `{code,message,data}`、`BizException`、极简 HS256 JWT、全局异常处理 |
| `mall-gateway` | 路由 + 鉴权；公开接口：登录、`GET /api/products/**`、`GET /api/categories/**`；会剥离外部伪造的身份头 |
| `mall-user` | 账号（H2 内存库） |
| `mall-product` | 商品目录（H2 内存库，确定性种子数据） |
| `mall-order` | 购物车 + 订单（H2 内存库） |

技术栈：Java 17、Spring Boot 4.0.8、Spring Cloud 2025.1、Spring Data JPA、H2。无注册中心（静态路由，地址可用环境变量覆盖）。

## 运行

```bash
./mvnw -q -DskipTests package       # 构建（首次会下载 Maven 与依赖）
bash scripts/start-all.sh           # 后台启动 4 个服务并等待健康检查，日志在 logs/
bash scripts/smoke-test.sh          # 30 项接口冒烟（需刚启动的干净数据）
bash scripts/stop-all.sh            # 按端口停止
# 或：docker compose up --build
```

**重置数据 = 重启服务**（全部为内存库）。

环境变量：`MALL_JWT_SECRET`、`MALL_USER_URL` / `MALL_PRODUCT_URL` / `MALL_ORDER_URL`、`MALL_*_DB_URL`（换成 MySQL/PostgreSQL 需自行加驱动）。

## 测试账号

| 用户名 | 密码 | 显示名 | 手机 |
|---|---|---|---|
| demo | tapestry2026 | 茶客_demo | 13800000001 |
| demo2 | tapestry2026 | 茶客_demo2 | 13800000002 |

## 接口一览（经网关 :8080）

| 方法 | 路径 | 登录 | 说明 | PRD |
|---|---|---|---|---|
| GET | `/api/categories` | | 分类列表 | REQ-011 |
| GET | `/api/products?category=` | | 商品列表 | REQ-010/011/012 |
| GET | `/api/products/{id}` | | 商品详情；不存在 404 `PRODUCT_NOT_FOUND` | REQ-013/014 |
| POST | `/api/auth/login` | | 登录，返回 `token`（7 天） | REQ-030 |
| GET | `/api/users/me` | ✓ | 当前用户 | REQ-032 |
| GET / DELETE | `/api/cart` | ✓ | 查看 / 清空购物车 | REQ-021 |
| POST | `/api/cart/items` | ✓ | 加购 `{productId, qty}`，同品合并，上限 99；售罄 409 `SOLD_OUT` | REQ-020 |
| PUT / DELETE | `/api/cart/items/{productId}` | ✓ | 改数量 / 删除 | REQ-021 |
| POST | `/api/orders/preview` | ✓ | 结算预览 `{source: CART\|DIRECT, items?}` | REQ-041 |
| POST | `/api/orders` | ✓ | 下单（含收货信息）；库存不足 409 `INSUFFICIENT_STOCK` | REQ-042/043 |
| GET | `/api/orders?status=` | ✓ | 我的订单 | REQ-050 |
| GET | `/api/orders/{orderNo}` | ✓ | 订单详情（仅本人） | REQ-051 |
| POST | `/api/orders/{orderNo}/pay` | ✓ | 模拟支付 | REQ-044 |
| POST | `/api/orders/{orderNo}/cancel` | ✓ | 取消待支付订单并回补库存 | REQ-052 |

业务规则：商品金额满 ¥99 包邮，否则运费 ¥10；订单号 `SM<yyyyMMdd><6 位序号>`；订单状态 `PENDING_PAYMENT → PAID | CANCELLED`。

错误码：`VALIDATION_FAILED` `BAD_REQUEST` `UNAUTHORIZED` `BAD_CREDENTIALS` `PRODUCT_NOT_FOUND` `SOLD_OUT` `INSUFFICIENT_STOCK` `NOTHING_TO_SETTLE` `CART_ITEM_NOT_FOUND` `ORDER_NOT_FOUND` `ORDER_STATUS_INVALID` `INTERNAL_ERROR`。
