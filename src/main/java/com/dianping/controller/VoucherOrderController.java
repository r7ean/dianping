package com.dianping.controller;


import com.dianping.dto.Result;
import com.dianping.service.IVoucherOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author roy
 * 
 */
@RestController
@RequestMapping("/voucher-order")
@Tag(name = "优惠券订单管理", description = "秒杀优惠券")
public class VoucherOrderController {
    
    @Resource
    private IVoucherOrderService voucherOrderService;

    /**
     * 新增秒杀优惠券订单
     * @param voucherId 优惠券ID
     * @return 优惠券订单ID
     */
    @PostMapping("seckill/{id}")
    @Operation(summary = "新增秒杀优惠券订单")
    public Result seckillVoucher(@PathVariable("id") Long voucherId) {
        return voucherOrderService.seckillVoucher(voucherId);
    }
}
