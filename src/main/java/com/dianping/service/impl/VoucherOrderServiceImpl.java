package com.dianping.service.impl;

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
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author roy
 * 
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

    private static final ThreadPoolExecutor SECKILL_ORDER_EXECUTOR = new ThreadPoolExecutor(
            1,
            2,
            5,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(5),
            Executors.defaultThreadFactory(),
            new ThreadPoolExecutor.AbortPolicy()
    );
    
    @Override
    public Result seckillVoucher(Long voucherId) {
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        //查看活动是否开启
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            return Result.fail("活动还未开始");
        }
        //查看活动是否过期
        if (voucher.getEndTime().isBefore(LocalDateTime.now())){
            return Result.fail("活动已结束");
        }
        //查看库存是否充足
        if (voucher.getStock()<1) {
            //不足，返回错误信息
            return Result.fail("库存不足");
        }
        
        Long userId = UserHolder.getUser().getId();
        SimpleRedisLock lock = new SimpleRedisLock(stringRedisTemplate, "order:" + userId);
        Boolean success = lock.tryLock(600L);
        if (!success) {
            seckillVoucher(voucherId);
        }
        try {
            proxy = (IVoucherOrderService) AopContext.currentProxy();
            //返回订单id
            return proxy.createVoucherOrder(voucherId, userId);
        }finally {
            lock.unLock();
        }
    }

    @Transactional
    public Result createVoucherOrder(Long voucherId, Long userId) {
        Integer count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        if (count > 0) {
            return Result.fail("用户已经购买过此优惠券!");
        }

        //充足，库存减一
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId)
                .gt("stock", 0)
                .update();
        if (!success) {
            return Result.fail("库存不足");
        }

        //新增订单
        VoucherOrder voucherOrder = new VoucherOrder();
        long OrderId = redisIdGenerator.nextId("order");
        voucherOrder.setId(OrderId);
        voucherOrder.setVoucherId(voucherId);
        voucherOrder.setUserId(UserHolder.getUser().getId());
        save(voucherOrder);
        return Result.ok(OrderId);
    }
}
