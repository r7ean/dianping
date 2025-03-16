package com.dianping.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class ThreadPoolConfig {
    @Bean(name = "seckillOrderExecutor", destroyMethod = "shutdown")
    public ThreadPoolExecutor seckillOrderExecutor() {
        return new ThreadPoolExecutor(
                1,
                2,
                3,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1024),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }
}
