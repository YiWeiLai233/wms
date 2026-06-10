package com.yiweilai.wms.product.entity;

import lombok.Data;

/**
 * 商品条码实体
 */
@Data
public class ProductBarcode {

    /** ID */
    private Long id;

    /** SKU ID */
    private Long skuId;

    /** 条码 */
    private String barcode;
}
