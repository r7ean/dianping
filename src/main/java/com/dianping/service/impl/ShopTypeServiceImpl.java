package com.dianping.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.json.JSONUtil;
import com.dianping.dto.Result;
import com.dianping.entity.ShopType;
import com.dianping.mapper.ShopTypeMapper;
import com.dianping.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.dianping.utils.RedisConstants.CACHE_SHOP_TYPE;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author roy
 * 
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public Result queryList() {
        // 1. 从redis中查询是否存在店铺类型的缓存
        List<String> shopTypeList = stringRedisTemplate.opsForList().range(CACHE_SHOP_TYPE, 0, -1);
        // 1.2 如果缓存数据不为空
        if (CollectionUtil.isNotEmpty(shopTypeList)){
            List<ShopType> list = new ArrayList<>();
            for(String s : shopTypeList){
                ShopType shopType = JSONUtil.toBean(s, ShopType.class);
                list.add(shopType);
            }
            return Result.ok(list);
        }
        // 2. 如果缓存数据为空 redis中没有数据
        List<ShopType> list = query().orderByAsc("sort").list();
        if(CollectionUtil.isEmpty(list)){
            return Result.fail("无商铺类型信息!");
        }
        // 2.1 将数据库中查出的 List<ShopType>的数据单独取出 封装成json格式
        shopTypeList = list.stream()
                .map((shopType) -> JSONUtil.toJsonStr(shopType))
                .collect(Collectors.toList());
        stringRedisTemplate.opsForList().rightPushAll(CACHE_SHOP_TYPE, shopTypeList);
        return Result.ok(list);
    }
}
