package com.dianping.utils;

import com.dianping.entity.Shop;
import com.dianping.service.IShopService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.dianping.utils.RedisConstants.SHOP_GEO_KEY;

@SpringBootTest
public class ImportShopGeoToRedis {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;

    @Resource
    private IShopService shopService;

    @Test
    public void writeShops(){
        List<Shop> shops = shopService.list();
        Map<Long,List<Shop>> map = shops.stream().collect(Collectors.groupingBy(Shop::getTypeId));

        for(Map.Entry<Long, List<Shop>> entry :map.entrySet()){
            Long typeId = entry.getKey();
            String key = SHOP_GEO_KEY + typeId;
            List<Shop> value = entry.getValue();

            List<RedisGeoCommands.GeoLocation<String>> locations = new ArrayList<>();

            for (Shop shop : value) {
                locations.add(
                        new RedisGeoCommands.GeoLocation<>(
                                shop.getId().toString()
                                , new Point(shop.getX(), shop.getY())
                        )
                );
            }

            stringRedisTemplate.opsForGeo().add(key, locations);
        }
    }
}
