package com.dianping.controller;


import com.dianping.dto.Result;
import com.dianping.entity.ShopType;
import com.dianping.service.IShopTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author roy
 * 
 */
@RestController
@RequestMapping("/shop-type")
@Tag(name = "商户类型管理", description = "商户类型列表")
public class ShopTypeController {
    @Resource
    private IShopTypeService typeService;

    /**
     * 查询商户类型列表
     * @return 商户类型列表
     */
    @GetMapping("list")
    @Operation(summary = "查询商户类型列表")
    public Result queryTypeList() {
        return typeService.queryList();
    }
}
