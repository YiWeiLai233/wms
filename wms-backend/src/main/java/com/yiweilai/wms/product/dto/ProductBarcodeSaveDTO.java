package com.yiweilai.wms.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 条码新增参数
 */
@Data
public class ProductBarcodeSaveDTO {

    /** SKU ID */
    @NotNull(message = "SKU ID不能为空")
    private Long skuId;

    /** 条码 */
    @NotBlank(message = "条码不能为空")
    private String barcode;
}
