package com.yiweilai.wms.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 商品新增/修改参数
 */
@Data
public class ProductSaveDTO {

    /** 商品ID（修改时必填） */
    private Long id;

    /** SPU编码 */
    @NotBlank(message = "SPU编码不能为空")
    private String spuCode;

    /** 商品名称 */
    @NotBlank(message = "商品名称不能为空")
    private String name;

    /** 分类ID（支持单个数字或级联数组） */
    private Object categoryId;

    /** 货架ID */
    @NotNull(message = "货架不能为空")
    private Long shelfId;

    /** 主图URL */
    private String mainImage;

    /** 商品描述 */
    private String description;

    /** 参考售价 */
    private BigDecimal price;

    /** 状态：0-禁用 1-启用 */
    private Integer status = 1;

    /** 创建商品时批量生成的码数 SKU 列表 */
    private List<SizeSkuDTO> skuList = new ArrayList<>();

    /**
     * 获取最终的分类ID
     * 支持级联选择器返回的数组格式 [1, 2, 3]，取最后一个元素
     */
    public Long getFinalCategoryId() {
        if (categoryId == null) {
            return null;
        }
        if (categoryId instanceof Number) {
            return ((Number) categoryId).longValue();
        }
        if (categoryId instanceof List<?> list && !list.isEmpty()) {
            Object last = list.get(list.size() - 1);
            if (last instanceof Number) {
                return ((Number) last).longValue();
            }
        }
        return null;
    }

    /**
     * 鞋服类商品的码数 SKU 录入项。
     */
    @Data
    public static class SizeSkuDTO {

        /** 码数，如 36、37、40 */
        @NotBlank(message = "码数不能为空")
        private String sizeValue;

        /** 可选 SKU 编码；为空时使用 SPU-码数 自动生成 */
        private String skuCode;

        /** 可选 SKU 名称；为空时使用 商品名称-码数 自动生成 */
        private String name;

        /** 初始库存数量 */
        private Integer quantity = 0;

        /** 成本价 */
        private BigDecimal costPrice;

        /** 售价 */
        private BigDecimal salePrice;

        /** 重量（kg） */
        private BigDecimal weight;

        /** 体积（m³） */
        private BigDecimal volume;

        /** SKU 图片 */
        private String image;
    }
}
