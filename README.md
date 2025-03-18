# Dianping Project

本项目使用SpringBoot框架进行开发的前后端分离项目，使用了redis、tomcat、MySQL等相关技术。类似于大众点评，实现了短信登录、商户查询缓存、优惠卷秒杀、附近的商户、UV统计、用户签到、好友关注、达人探店 八个部分形成了闭环。其中重点使用了分布式锁实现了一人一单功能、项目中大量使用了Redis 的知识。

[TOC]

## 准备工作

### 持久层数据库准备

#### 在 本机/虚拟机/Docker 中启动MySQL服务

创建相关数据库

`CREATE DATABASE dianping;`  

导入数据库，将`./src/resources/db/dianping.sql` 导入到创建好的数据库中
   
   ```
   tb_user: 用户表
   tb_user_info: 用户详情表
   tb_shop: 商户信息表
   tb_shop_type: 商户类型表
   tb_blog: 用户博客表
   tb_fellow: 用户关注表
   tb_voucher: 优惠券表
   tb_voucher_order: 购买优惠券订单表
   ```
   
#### 在 本机/虚拟机/Docker 启动redis服务

指定 redis 端口号为 6379， 本项目未设置 redis 密码。

创建项目中需要的 Stream 类型的消息队列, 名为 `streams.order`  

> XGROUP CREATE streams.orders consumerGroup 0 MKSTREAM

提前将商户GEO数据导入Redis中, 以免项目启动后无法查询到商户信息, 运行 `.src/test/java/com/dianping/utils/ImportShopGeoToRedis.java` 文件

### 前端部署

前端项目文件位置: `./src/resources/dianping-frontend`

nginx 配置文件: `./src/resources/dianping-frontend/conf/nginx.conf` 

运行前端项目: 双击运行 `nginx.exe` 文件

### 项目配置  

#### 配置 `application.yaml` 

```yaml
server:
  port: 8081 # 指定端口号为8081, nginx.conf中代理的也是这个端口, 如果更改也需要更改nginx.conf中的配置
  shutdown: graceful
spring:
  lifecycle:
    timeout-per-shutdown-phase: 2s # 退出spring时 等待关闭资源时间
  application:
    name: dianping
  datasource:
    driver-class-name: com.mysql.jdbc.Driver
    url: jdbc:mysql://127.0.0.1:3307/dianping?useSSL=false&serverTimezone=UTC&characterEncoding=utf8&useUnicode=true # 配置数据库连接 url, 我这里使用的端口号是 3307
    username: { username }
    password: { password }
  redis:
    host: 127.0.0.1
    port: 6379
    lettuce:
      pool:
        max-active: 10
        max-idle: 10
        min-idle: 1
        time-between-eviction-runs: 10s
    database: 2 # 使用redis哪个仓库 {0 .. 15}
  jackson:
    default-property-inclusion: non_null # JSON处理时忽略非空字段
mybatis-plus:
  type-aliases-package: com.dianping.entity # 别名扫描包
logging:
  level:
    com.dianping: debug
```

## 🚀项目启动

运行 `./src/java/com/dianping/DianpingApplication.java`, 项目运行在8081端口 

使用浏览器访问 [🎈dianping-frontend](http://localhost:8080/)

### 测试秒杀优惠券功能

使用 postman 调用`http://localhost:8081/voucher/seckill` 接口, 设置request body为

```json
    {
      "shopId":1,
      "title":"100元代金券",
      "subTitle":"周一至周五可用",
      "rules":"全场通用\\n无需预约\\n不可叠加\\不兑现、不找零\\n仅限堂食",
      "payValue":7900,
      "actualValue":10000,
      "type":1,
      "stock":100,
      "benginTime": "2025-03-15T20:00:00",
      "endTime": "2025-04-01T20:00:00"
    }
```

账号登录, 进入相关商户, 点击抢购优惠券
