package com.yiweilai.wms.express.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 快递费用模板返回对象
 */
@Data
public class ExpressFeeTemplateVO {

    /** 模板ID */
    private Long id;

    /** 快递公司ID */
    private Long companyId;

    /** 快递公司名称 */
    private String companyName;

    /** 模板名称 */
    private String name;

    /** 模板类型：LADDER-阶梯计费, FIRST_CONTINUE-首重续重 */
    private String templateType;

    /** 首重重量(kg)，首重续重类型使用 */
    private BigDecimal firstWeight;

    /** 首重费用(元)，首重续重类型使用 */
    private BigDecimal firstFee;

    /** 续重重量(kg)，首重续重类型使用 */
    private BigDecimal additionalWeight;

    /** 续重费用(元/kg)，首重续重类型使用 */
    private BigDecimal additionalFee;

    /** 是否默认模板 */
    private Integer isDefault;

    /** 状态：1-启用 0-禁用 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 费用阶梯列表 */
    private List<FeeStepVO> steps;

    /**
     * 费用阶梯项
     */
    @Data
    public static class FeeStepVO {

        /** 阶梯ID */
        private Long id;

        /** 最小重量(kg) */
        private BigDecimal minWeight;

        /** 最大重量(kg) */
        private BigDecimal maxWeight;

        /** 费用(元) */
        private BigDecimal fee;

        /** 排序 */
        private Integer sortOrder;
    }
}
