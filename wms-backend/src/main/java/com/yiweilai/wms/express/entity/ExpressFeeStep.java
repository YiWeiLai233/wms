package com.yiweilai.wms.express.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 快递费用阶梯实体
 */
@Data
public class ExpressFeeStep {

    /** 阶梯ID */
    private Long id;

    /** 模板ID */
    private Long templateId;

    /** 最小重量(kg)，含 */
    private BigDecimal minWeight;

    /** 最大重量(kg)，不含 */
    private BigDecimal maxWeight;

    /** 费用(元) */
    private BigDecimal fee;

    /** 排序 */
    private Integer sortOrder;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
