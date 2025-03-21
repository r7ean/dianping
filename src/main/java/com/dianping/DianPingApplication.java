package com.dianping;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@MapperScan("com.dianping.mapper")
@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true)
public class DianPingApplication {
    public static void main(String[] args) {
        SpringApplication.run(DianPingApplication.class, args);
    }

}
