package com.yiweilai.wms.product.vo;

import lombok.Data;

/**
 * 条码返回对象
 */
@Data
public class ProductBarcodeVO {

    /** ID */
    private Long id;

    /** SKU ID */
    private Long skuId;

    /** 条码 */
    private String barcode;
}
