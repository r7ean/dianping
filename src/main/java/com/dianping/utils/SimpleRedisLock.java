package com.dianping.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock{

    private StringRedisTemplate stringRedisTemplate;
    private String keyName;
    private static final String KEY_PREFIX = "Lock:";
    private static final String THREAD_ID_PREFIX = UUID.randomUUID().toString(true) + "-";

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    
    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    public SimpleRedisLock(StringRedisTemplate stringRedisTemplate, String keyName) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.keyName = keyName;
    }

    @Override
    public Boolean tryLock(Long timeoutSec) {
        String key = KEY_PREFIX + keyName;
        String ThreadId = THREAD_ID_PREFIX + Thread.currentThread().getId();
        return stringRedisTemplate.opsForValue().setIfAbsent(key, ThreadId, timeoutSec, TimeUnit.SECONDS);
    }

    public void unLock(){
        // 使用lua脚本释放锁 保证获取锁的value和释放锁的原子性
        stringRedisTemplate.execute(
                UNLOCK_SCRIPT,
                Collections.singletonList(KEY_PREFIX + keyName),
                THREAD_ID_PREFIX + Thread.currentThread().getId()
        );
    }

//    @Override
//    public void unLock() {
//        String ThreadId = THREAD_ID_PREFIX + Thread.currentThread().getId();
//        String key = KEY_PREFIX + keyName;
//        String ThreadIdKey = stringRedisTemplate.opsForValue().get(key);
//        if(ThreadId.equals(ThreadIdKey)){
//            stringRedisTemplate.delete(key);
//        }
//    }
}
