package com.dianping.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.dianping.utils.RedisConstants.*;

@Slf4j
@Component
public class CacheClient {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // 解决缓存击穿问题使用的线程池
    private static final ThreadPoolExecutor REBUILD_DATA_THREAD = new ThreadPoolExecutor(
            3,
            5,
            2,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(3),
            Executors.defaultThreadFactory(),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    /**
     * 设置redis缓存
     * @param key
     * @param value
     * @param time
     * @param unit
     */
    public void set(String key, Object value, Long time, TimeUnit unit){
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value),time,unit);
    }

    /**
     * 设置redis 热点key 逻辑过期
     * @param key
     * @param value
     * @param time
     * @param unit
     */
    public void setLogicalExpire(String key, Object value, Long time, TimeUnit unit){
        // 设置逻辑过期
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    /**
     * redis 查询解决缓存穿透
     * @param keyPrefix key前缀
     * @param id    id
     * @param type  返回值类型
     * @param time  存入redis中的时间长度
     * @param unit  存入redis中的时间单位
     * @param dbFallback    函数接口 执行查询数据库操作
     * @return
     * @param <R>
     * @param <T>
     */
    public <R, T> R queryWithPassThrough(String keyPrefix,
                                         T id, 
                                         Class<R> type,
                                         Long time,
                                         TimeUnit unit,
                                         Function<T, R> dbFallback) {
        // 1. 获取需要查询的key
        String key = keyPrefix + id;
        // 2. 从redis中根据key查询数据
        String resJson = stringRedisTemplate.opsForValue().get(key);
        // 2.1 判断返回的数据json是否为空
        if (StrUtil.isNotBlank(resJson)) {
            // 反序列化为对象 然后返回
            return JSONUtil.toBean(resJson, type);
        }
        // 3. 解决缓存穿透问题，如果当前json为""
        if (resJson != null) {
            // 如果当前json为null 说明此时是为了解决缓存穿透存入的数据 直接返回null
            return null;
        }
        // 4. 如果当前redis确实没有值，json为null，则需要去数据库查询数据
        R resData = dbFallback.apply(id);
        // 4.1 如果数据库中没有此数据
        if (resData == null) {
            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.SECONDS);
            return null;
        }
        // 4.2 如果数据库中有此数据
        // 则存入redis
        set(key, resData,time,unit);
        return resData;
    }
    /**
     * 解决缓存击穿—— 逻辑过期
     * @param keyPrefix
     * @param id
     * @param type
     * @param time
     * @param unit
     * @param dbFallback
     * @return
     * @param <R>
     * @param <T>
     */
    public <R, T> R queryWithLogicalExpire4CacheBreakdown(String keyPrefix, 
                                                          T id, 
                                                          Class<R> type,
                                                          Long time,
                                                          TimeUnit unit,
                                                          Function<T, R> dbFallback) {
        // 1. 从redis中查询缓存
        // 构建key
        String key = keyPrefix + id;
        String resJson = stringRedisTemplate.opsForValue().get(key);
        // 2. 判断缓存是否存在
        if (StrUtil.isBlank(resJson)) {
            // 2.1 不存在 则直接返回
            return null;
        }
        // 3. 如果存在 将redis中获取的JSON反序列化为对象
        RedisData redisData = JSONUtil.toBean(resJson, RedisData.class);
        R resData = JSONUtil.toBean((JSONObject) redisData.getData(), type);

        // 4. 检查是否数据是否已经逻辑过期
        // 4.1 如果数据未过期
        if (redisData.getExpireTime().isAfter(LocalDateTime.now())) {
            // 如果过期时间在现在之后 说明数据没有过期 直接返回
            return resData;
        }
        // 5. 如果数据已经过期 则需要重建数据
        // 5.1 先给当前数据加锁
        String lockKey = LOCK_SHOP_KEY + id;
        Boolean isLock = tryLock(lockKey);
        if (isLock) {
            // 5.2 如果成功拿到锁 就重建数据
            try {
                REBUILD_DATA_THREAD.submit(()->{
                    // 5.2.1 重建数据需要先查询数据 拿到数据库数据 
                    R r = dbFallback.apply(id);
                    // 5.2.2 将数据写入redis
                    setLogicalExpire(key,r,time,unit);
                });
            } finally {
                REBUILD_DATA_THREAD.shutdown();
                unLock(lockKey);
            }
        }
        // 6. 如果没有拿到锁 直接返回过期数据
        return resData;
    }

    private Boolean tryLock(String key){
        Boolean lock = stringRedisTemplate.opsForValue().setIfAbsent(key, "locked", LOCK_SHOP_TTL, TimeUnit.MINUTES);
        return BooleanUtil.isTrue(lock);
    }
    private void unLock(String key){
        stringRedisTemplate.delete(key);
    }
}
