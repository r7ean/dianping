package com.dianping.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 
 * </p>
 *
 * @author roy
 * 
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_voucher")
@Schema(description = "优惠券实体")
public class Voucher implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 商铺id
     */
    @Schema(description = "商户ID", example = "1")
    private Long shopId;

    /**
     * 代金券标题
     */
    @Schema(description = "优惠券标题", example = "100元代金券")
    private String title;

    /**
     * 副标题
     */
    @Schema(description = "副标题", example = "周一至周五可用")
    private String subTitle;

    /**
     * 使用规则
     */
    @Schema(description = "使用规则", example = "全场通用\\\\n无需预约\\\\n不可叠加\\\\不兑现、不找零\\\\n仅限堂食")
    private String rules;

    /**
     * 支付金额
     */
    @Schema(description = "支付金额，单位是分。例如200代表2元", example = "5900")
    private Long payValue;

    /**
     * 抵扣金额
     */
    @Schema(description = "抵扣金额，单位是分。例如200代表2元", example = "10000")
    private Long actualValue;

    /**
     * 优惠券类型
     */
    @Schema(description = "0,普通券；1,秒杀券", example = "0")
    private Integer type;

    /**
     * 优惠券状态
     */
    @Schema(description = "1,上架; 2,下架; 3,过期", example = "1")
    private Integer status;
    /**
     * 库存
     */
    @TableField(exist = false)
    @Schema(description = "优惠券库存, 当优惠券类型为秒杀券时才设置", example = "100")
    private Integer stock;

    /**
     * 生效时间
     */
    @TableField(exist = false)
    @Schema(description = "生效时间,当优惠券类型为秒杀券时才设置", example = "2025-03-15T20:00:00")
    private LocalDateTime beginTime;

    /**
     * 失效时间
     */
    @TableField(exist = false)
    @Schema(description = "失效时间,当优惠券类型为秒杀券时才设置", example = "2026-03-15T20:00:00")
    private LocalDateTime endTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;


    /**
     * 更新时间
     */
    private LocalDateTime updateTime;


}
