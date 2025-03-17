package com.dianping.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianping.dto.Result;
import com.dianping.entity.Shop;
import com.dianping.mapper.ShopMapper;
import com.dianping.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dianping.utils.CacheClient;
import com.dianping.utils.RedisData;
import com.dianping.utils.SystemConstants;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.dianping.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author roy
 * 
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private CacheClient cacheClient;

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
    @Override
    public Result queryById(Long id) {
        Shop shop = cacheClient.queryWithPassThrough(
                CACHE_SHOP_KEY,
                id,
                Shop.class,
                CACHE_SHOP_TTL,
                TimeUnit.MINUTES,
                this::getById
        );
        if (shop == null) {
            return Result.fail("店铺不存在!");
        }
        return Result.ok(shop);
    }

    @Override
    public Result updateShop(Shop shop) {
        Long shopId = shop.getId();
        if (shopId == null) {
            return Result.fail("商铺ID不能为空!");
        }
        updateById(shop);

        String key = CACHE_SHOP_KEY + shopId;
        stringRedisTemplate.delete(key);
        return Result.ok();
    }

    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
        // 1. 如果不需要根据坐标查询
        if (x == null || y == null) {
            List<Shop> shops = query()
                    .eq("type_id", typeId)
                    .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE))
                    .getRecords();
            return Result.ok(shops);
        }
        // 2. 计算分页
        int from = (current - 1) * SystemConstants.DEFAULT_PAGE_SIZE;
        int end = current * SystemConstants.DEFAULT_PAGE_SIZE;
        // 3. 查询redis 按照距离排序 分页
        String key = SHOP_GEO_KEY + typeId;
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo().search(
                key,
                GeoReference.fromCoordinate(x, y),
                new Distance(5000),
                RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().limit(end)
        );
        if(results == null){
            return Result.ok(Collections.emptyList());
        }
        // 4. 解析id
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> list = results.getContent();
        if(list.size() <= from){
            return Result.ok(Collections.emptyList());
        }
        List<Long> ids = new ArrayList<>();
        Map<String, Distance> distanceMap = new HashMap<>();
        list.stream().skip(from).forEach(result ->{
            // 获取店铺id
            String shopId = result.getContent().getName();
            ids.add(Long.valueOf(shopId));
            // 获取距离
            Distance distance = result.getDistance();
            distanceMap.put(shopId,distance);
        });
        // 5. 根据id查询shop
        String strIds = StrUtil.join(",", ids);
        List<Shop> shops = query()
                .in("id", ids)
                .last("order by field(id," + strIds + ")")
                .list();

        for (Shop shop : shops) {
            shop.setDistance(
                    distanceMap.
                            get(shop.getId().toString())
                            .getValue()
            );
        }
        return Result.ok(shops);
    }

    // 缓存击穿解决方案 使用逻辑过期
    public Shop QueryCacheBreakDownLogicalExpire(Long id){
        // 1. 从redis查询商铺缓存
        String shopKey = CACHE_SHOP_KEY + id;
        String shopJson = stringRedisTemplate.opsForValue().get(shopKey);
        // 2. 判断缓存是否存在
        if(StrUtil.isBlank(shopJson)){
            // 2.1 不存在 则直接返回
            return null;
        }
        // 将redis中获取的JSON反序列化为对象
        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
        Shop shop = JSONUtil.toBean((JSONObject) redisData.getData(), Shop.class);

        if(redisData.getExpireTime().isAfter(LocalDateTime.now())){
            // 如果过期时间在现在之后 说明数据没有过期 直接返回
            return shop;
        }
        // 如果过期时间在现在之前 说明数据已经逻辑过期 需要重建数据
        String lockKey = LOCK_SHOP_KEY + id;
        Boolean isLock = tryLock(lockKey);
        if (isLock){
            try {
                // DoubleCheck
                shopJson = stringRedisTemplate.opsForValue().get(shopKey);
                redisData = JSONUtil.toBean(shopJson, RedisData.class);
                if(redisData.getExpireTime().isAfter(LocalDateTime.now())){
                    // 如果过期时间在现在之后 说明数据没有过期 直接返回
                    return shop;
                }
                // 如果获取锁成功 那么开启独立线程 重建数据
                REBUILD_DATA_THREAD.submit(()->{
                    saveShop2Redis(id, CACHE_SHOP_TTL);
                });
            }finally {
                REBUILD_DATA_THREAD.shutdown();
                unLock(lockKey);
            }
        }
        // 6. 直接返回过期数据
        return shop;
    }

    //互斥锁解决缓存击穿
    public Shop queryWithMutex(Long id){
        String key = CACHE_SHOP_KEY + id;
        //在redis中查询商户
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        //存在，则返回
        if (StrUtil.isNotBlank(shopJson)){
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return shop;
        }
        //如果redis中为""空字符串,返回失败
        if (shopJson != null){
            return null;
        }
        //缓存重建
        //获取锁
        String lockKey = "lock:shop:"+id;
        Shop shop = null;
        try {
            boolean isLocked = tryLock(lockKey);
            //判断是否获取
            if (!isLocked) {
                //没获取，重试
                Thread.sleep(50);
                return queryWithMutex(id);
            }
            //获取，开始重建
            //获取锁后判断缓存是否能命中，即之前获取锁的进程是否修改redis
            String s = stringRedisTemplate.opsForValue().get(key);
            if (StrUtil.isNotBlank(s)){
                shop = JSONUtil.toBean(s, Shop.class);
                return shop;
            }
            //如果redis中为""空字符串,返回失败
            if (s != null){
                return null;
            }
            //不存在,在mysql数据库中查询
            shop = getById(id);
            //模拟延迟
            Thread.sleep(200);
            //不存在，返回错误信息
            if (shop == null){
                //把空值写到redis，防止缓存穿透
                stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL,TimeUnit.MINUTES);
                return null;
            }
            //存在，将查询的数据放到redis中
            stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL,TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }finally {
            //释放锁
            unLock(lockKey);
        }

        //返回信息
        return shop;
    }
    
    // 解决缓存穿透
    public Shop QueryCachePassThrough(Long id){
        // 1. 从redis查询商铺缓存
        String shopKey = CACHE_SHOP_KEY + id;
        String shopJson = stringRedisTemplate.opsForValue().get(shopKey);
        // 2. 判断缓存是否存在
        if(StrUtil.isNotBlank(shopJson)){
            // 2.1 存在 则直接返回
            return JSONUtil.toBean(shopJson, Shop.class);
        }

        if(shopJson != null){
            return null;
        }

        // 3. 不存在 则根据ID查询商铺
        Shop shop = getById(id);
        // 4. 从数据库查出数据
        if(shop == null){
            // 4.1 不存在 返回错误
            stringRedisTemplate.opsForValue().set(shopKey, "",CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        // 5. 存在 存入redis
        stringRedisTemplate.opsForValue().set(shopKey, JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);
        // 6. 返回
        return shop;
    }

    private Boolean tryLock(String key){
        Boolean lock = stringRedisTemplate.opsForValue().setIfAbsent(key, "lock", LOCK_SHOP_TTL, TimeUnit.MINUTES);
        return BooleanUtil.isTrue(lock);
    }
    private void unLock(String key){
        stringRedisTemplate.delete(key);
    }

    private void saveShop2Redis(Long id, Long expireSeconds){
        // 1. 查询店铺数据
        Shop shop = getById(id);
        // 2. 封装逻辑过期时间 封装redis对象
        RedisData redisData = new RedisData(shop, LocalDateTime.now().plusSeconds(expireSeconds));
        // 3. 写入redis
        String shopKey = CACHE_SHOP_KEY + id;
        stringRedisTemplate.opsForValue().set(shopKey,JSONUtil.toJsonStr(redisData));
    }
}
