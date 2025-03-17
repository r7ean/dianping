## 仿大众点评项目

### 本地运行

1. 配置 `application.yaml` 

```yaml
server:
  port: 8081
  shutdown: graceful
spring:
  lifecycle:
    timeout-per-shutdown-phase: 2s #退出spring时 等待关闭资源时间
  application:
    name: dianping
  datasource:
    driver-class-name: com.mysql.jdbc.Driver
    url: jdbc:mysql://127.0.0.1:3307/dianping?useSSL=false&serverTimezone=UTC&characterEncoding=utf8&useUnicode=true
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

2. 根据上述`application.yaml`配置文件信息创建mysql 和 redis。  
2.1 我使用的`host`为本机`127.0.0.1`, mysql端口号为3307, redis端口号为6379， 需要根据自己实际情况修改。  

3. 执行`./resource/db/dianping.sql` 文件, 初始化数据库。  

4. 初始化redis中数据  
4.1 使用postman软件，调用`http://localhost:8081/voucher/seckill`, 设置requst body为
    ```
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