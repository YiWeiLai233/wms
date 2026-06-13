package com.yiweilai.wms.express.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 快递查询参数
 */
@Data
public class ExpressQueryDTO {

    /** 快递单号 */
    private String trackingNo;

    /** 快递公司编码 */
    private String carrier;

    /** 费用模板ID */
    private Long templateId;

    /** 总重量（kg） */
    private BigDecimal totalWeight;

    /** 总数量 */
    private Integer totalQuantity;
}
