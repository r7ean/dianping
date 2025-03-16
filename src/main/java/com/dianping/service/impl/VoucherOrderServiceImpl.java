package com.dianping.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.dianping.dto.Result;
import com.dianping.entity.SeckillVoucher;
import com.dianping.entity.VoucherOrder;
import com.dianping.mapper.VoucherOrderMapper;
import com.dianping.service.ISeckillVoucherService;
import com.dianping.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dianping.utils.RedisIdGenerator;
import com.dianping.utils.SimpleRedisLock;
import com.dianping.utils.UserHolder;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author roy
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedisIdGenerator redisIdGenerator;

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private RedissonClient redissonClient;

    private IVoucherOrderService proxy;

    @Resource
    @Qualifier("seckillOrderExecutor")
    private ThreadPoolExecutor seckillOrderExecutor;

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    private String messageQueueName = "stream.orders";

    // 秒杀lua脚本
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    @PostConstruct
    private void init() {
        seckillOrderExecutor.submit(() -> {
            while (true) {
                try {
                    // 获取redis中消息队列的订单信息 xreadgroup group consumerGroup consumer count 1 block 2s streams streams.orders >
                    List<MapRecord<String, Object, Object>> messageList = 
                            stringRedisTemplate.opsForStream().read(
                            Consumer.from("consumerGroup", "consumer"),
                            StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                            StreamOffset.create(messageQueueName, ReadOffset.lastConsumed())
                    );
                    
                    if (messageList == null || messageList.isEmpty()) {
                        continue;
                    }

                    MapRecord<String, Object, Object> currentMessage = messageList.get(0);
                    Map<Object, Object> orderValues = currentMessage.getValue();

                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(orderValues, new VoucherOrder(), true);
                    handlerVoucherOrder(voucherOrder);

                    // 消息队列 ACK 确认 SACK streams.order consumerGroup id
                    stringRedisTemplate.opsForStream().acknowledge(messageQueueName, "consumerGroup", currentMessage.getId());
                    
                } catch (Exception error) {
                    log.error("处理订单异常!",error);
                    handlerPendingList();
                }
            }
        });
    }

    /**
     * 处理消息队列中的消息
     */
    private void handlerPendingList() {
        while (true) {
            try {
                // 1. 获取pending-list消息队列中的订单消息
                List<MapRecord<String, Object, Object>> messageList = stringRedisTemplate.opsForStream().read(
                        Consumer.from("consumerGroup", "consumer"),
                        StreamReadOptions.empty().count(1),
                        StreamOffset.create(messageQueueName, ReadOffset.from("0"))
                );
                // 2. 判断消息获取是否成功
                if (CollectionUtil.isEmpty(messageList)) {
                    break;
                }
                // 3. 解析消息列表
                MapRecord<String, Object, Object> currentMessage = messageList.get(0);
                Map<Object, Object> orderValues = currentMessage.getValue();
                
                VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(orderValues, new VoucherOrder(), true);
                voucherOrder.setCreateTime(LocalDateTime.now());
                
                // 4. 如果获取成功 可以下单
                handlerVoucherOrder(voucherOrder);
                // 5. ACK确认
                stringRedisTemplate.opsForStream().acknowledge(messageQueueName, "consumerGroup", currentMessage.getId());
            } catch (Exception error) {
                log.error("处理订单异常!", error);
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
    
    /**
     * 异步线程处理订单
     * @param voucherOrder
     */
    private void handlerVoucherOrder(VoucherOrder voucherOrder) {
        Long userId = voucherOrder.getUserId();
        RLock lock = redissonClient.getLock("Lock:Order:" + userId);
        boolean isLock = lock.tryLock();
        if(!isLock){
            // 获取锁失败 返回错误
            log.error("不允许重复下单!");
            return;
        }
        try{
            // 8. 返回订单id
            proxy.createVoucherOrder(voucherOrder);
        }finally {
            lock.unlock();
        }
    }
    
    @Override
    public Result seckillVoucher(Long voucherId) {
        // 获取用户id
        Long userId = UserHolder.getUser().getId();
        Long voucherOrderId = redisIdGenerator.nextId("VoucherOrder");
        // 使用lua脚本 在redis中查询库存
        Long executeResult = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(),
                userId.toString(),
                String.valueOf(voucherId)
        );

        // 如果执行结果不为0 说明库存不足或者 用户已下单
        if(executeResult != null && !executeResult.equals(0L)){
            return Result.fail(executeResult.equals(1L) ? "优惠券库存不足!":"每个用户只能购买一单!");
        }
        // 3. 写入数据库
        proxy = (IVoucherOrderService) AopContext.currentProxy();
        return Result.ok(voucherOrderId);
    }

    // 创建订单
    @Transactional
    public void  createVoucherOrder(VoucherOrder voucherOrder) {
        Integer count = query()
                .eq("user_id", voucherOrder.getUserId())
                .eq("voucher_id", voucherOrder.getVoucherId())
                .count();
        
        if (count > 0) {
            log.error("每个用户只能购买一单!");
        }

        //充足，库存减一
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherOrder.getVoucherId())
                .gt("stock", 0)
                .update();
        if (!success) {
            log.error("优惠券已无库存!");
        }
        // 7. 创建订单
        save(voucherOrder);
    }


    /**
     * 销毁方法，优雅停线程
     */
    @PreDestroy
    public void destroy() {
        seckillOrderExecutor.shutdown(); // 停止接收新任务
        try {
            if (!seckillOrderExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                System.out.println("Forcing shutdown...");
                seckillOrderExecutor.shutdownNow(); // 强制关闭
            }
        } catch (InterruptedException e) {
            seckillOrderExecutor.shutdownNow();
        }
        System.out.println("ThreadPool shutdown complete");
    }
}
