package com.dianping.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class RedisIdGenerator {
    
    // 初始时间时间戳 2024-01-01 0:00
    private static final long BEGIN_TIMESTAMP = 1704067200L;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final int COUNT_BITS = 32;

    /**
     * 生成ID
     * @param keyPrefix
     * @return
     */
    public Long nextId(String keyPrefix){
        // 1. 生成时间戳
        LocalDateTime now = LocalDateTime.now();
        long timeStamp = now.toEpochSecond(ZoneOffset.UTC) - BEGIN_TIMESTAMP;
        // 2. 生成序列号
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        Long count = stringRedisTemplate.opsForValue().increment("incr:" + keyPrefix + ":" + date);
        // 3. 拼接并返回
        return timeStamp << COUNT_BITS | count;
    }
}
