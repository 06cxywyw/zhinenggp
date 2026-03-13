# 智能票务购票系统

![Java](https://img.shields.io/badge/Java-8-orange)
![Spring Boot](https://img.shields.io/badge/SpringBoot-2.3-green)
![MyBatis-Plus](https://img.shields.io/badge/MyBatis--Plus-3.4-blue)
![MySQL](https://img.shields.io/badge/MySQL-8-blue)
![Redis](https://img.shields.io/badge/Redis-6-red)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3-orange)
![License](https://img.shields.io/badge/license-MIT-brightgreen)

---

基于 **Spring Boot** 的高并发电商点评系统，支持 **秒杀、优惠券、探店笔记、关注推送等功能**。

系统通过 **Redis、RabbitMQ、Lua 脚本以及分布式锁** 解决高并发场景中的库存超卖、请求洪峰以及缓存问题，并结合 **多级缓存架构** 提升系统整体性能。

---

# 技术栈

## 后端框架
- **Spring Boot** 2.3.12.RELEASE
- **MyBatis-Plus** 3.4.3
- **Java** 1.8

## 中间件
- **MySQL** 8.0 — 数据存储
- **Redis** — 缓存、分布式锁
- **RabbitMQ** — 消息队列、异步解耦

## 核心依赖
- **Redisson** 3.17.5 — 分布式锁
- **Caffeine** 2.9.3 — 本地缓存
- **Hutool** 5.7.17 — 工具类库
- **Lombok** — 简化代码

---

---

# 系统架构

```
                Client
                  │
                  ▼
               Nginx
                  │
                  ▼
        Docker Container Network
                  │
        ┌─────────┼─────────┐
        ▼         ▼         ▼
   Spring Boot   Redis     RabbitMQ
   Application  (缓存)     (消息队列)
        │
        ▼
      MySQL
   (数据存储)
```

系统采用 **Docker 容器化部署 + Spring Boot 微服务架构**：

- **Docker**：容器化部署应用及中间件，保证环境一致性
- **Nginx**：反向代理与请求转发
- **Spring Boot**：核心业务服务
- **Redis**：缓存热点数据、实现分布式锁
- **RabbitMQ**：削峰填谷，实现秒杀异步下单
- **MySQL**：持久化业务数据

通过 **Redis + RabbitMQ + Lua 脚本 + 分布式锁** 实现高并发秒杀场景优化，并利用 **Docker 容器化部署**提升系统可移植性与部署效率。

---
# 核心功能

## 用户模块
- 手机号登录 / 注册
- 用户信息维护

## 商户模块
- 店铺查询与展示
- 店铺类型管理
- 缓存优化解决方案：
    - 缓存穿透
    - 缓存击穿
    - 缓存雪崩

## 优惠券模块
- 普通优惠券领取
- **秒杀优惠券（高并发优化）**
    - Lua 脚本 + Redis 原子扣减库存
    - RabbitMQ 异步下单
    - Redisson 分布式锁防止超卖

## 笔记模块
- 探店笔记发布
- 点赞功能
- 评论互动
- 关注推送（Feed 流）

## 关注模块
- 关注 / 取关功能
- 共同关注查询
- Feed 流信息推送

---

# 项目结构

```
hm-dianping/
├── src/main/java/com/hmdp/
│   ├── config/           # 配置类 (Redis、RabbitMQ、MyBatis 等)
│   ├── controller/       # 控制器层
│   ├── service/          # 服务接口
│   │   └── impl/         # 服务实现
│   ├── mapper/           # MyBatis Mapper
│   ├── entity/           # 实体类
│   ├── dto/              # 数据传输对象
│   └── utils/            # 工具类 (Redis 锁、ID 生成器等)
│
├── src/main/resources/
│   ├── application.yaml  # 配置文件
│   ├── seckill.lua       # 秒杀 Lua 脚本
│   └── stock.lua         # 库存 Lua 脚本
│
└── pom.xml
```

---

# 快速开始

## 环境要求

- JDK 1.8+
- MySQL 8.0+
- Redis
- RabbitMQ

---

## 配置说明

编辑：

```
src/main/resources/application.yaml
```

示例配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/hmdp
    username: your_username
    password: your_password

  redis:
    host: localhost
    port: 6379
    password: your_password

  rabbitmq:
    host: localhost
    port: 5672
    username: your_username
    password: your_password
```

---

# 启动项目

```bash
mvn spring-boot:run
```

服务默认运行地址：

```
http://localhost:8081
```

---

# 核心技术亮点

## 1 多级缓存架构

- **Caffeine 本地缓存 + Redis 分布式缓存**
- 互斥锁解决缓存击穿
- 逻辑过期实现高可用缓存

---

## 2 高并发秒杀

- Lua 脚本保证 Redis 操作原子性
- RabbitMQ 异步削峰填谷
- Redisson 分布式锁防止重复下单

---

## 3 Feed 流推送

- 采用 **推模式** 实现关注推送
- 通过 **滚动分页查询** 提升查询效率

---

# License

MIT License