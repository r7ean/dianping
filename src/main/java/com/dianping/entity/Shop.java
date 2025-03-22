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
@TableName("tb_shop")
@Schema(description = "商户实体")
public class Shop implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 商铺名称
     */
    @Schema(description = "商户名称", example = "商户名称")
    private String name;

    /**
     * 商铺类型的id
     */
    @Schema(description = "商户类型id", example = "1")
    private Long typeId;

    /**
     * 商铺图片，多个图片以','隔开
     */
    @Schema(description = "商户图片url", example = "url, url")
    private String images;

    /**
     * 商圈，例如陆家嘴
     */
    @Schema(description = "商圈", example = "area")
    private String area;

    /**
     * 地址
     */
    @Schema(description = "地址", example = "地址")
    private String address;

    /**
     * 经度
     */
    @Schema(description = "经度", example = "3.15648")
    private Double x;

    /**
     * 维度
     */
    @Schema(description = "纬度", example = "5.15646")
    private Double y;

    /**
     * 均价，取整数
     */
    @Schema(description = "均价", example = "81")
    private Long avgPrice;

    /**
     * 销量
     */
    @Schema(description = "销量", example = "100")
    private Integer sold;

    /**
     * 评论数量
     */
    @Schema(description = "评论数量", example = "1")
    private Integer comments;

    /**
     * 评分，1~5分，乘10保存，避免小数
     */
    @Schema(description = "评分", example = "38")
    private Integer score;

    /**
     * 营业时间，例如 10:00-22:00
     */
    @Schema(description = "营业时间", example = "10:00 - 22:00")
    private String openHours;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;


    @TableField(exist = false)
    private Double distance;
}
