package com.yiweilai.wms.express.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 快递费用模板新增/修改参数
 */
@Data
public class ExpressFeeTemplateSaveDTO {

    /** 模板ID（修改时必填） */
    private Long id;

    /** 快递公司ID */
    @NotNull(message = "快递公司不能为空")
    private Long companyId;

    /** 模板名称 */
    @NotBlank(message = "模板名称不能为空")
    private String name;

    /** 模板类型：LADDER-阶梯计费, FIRST_CONTINUE-首重续重 */
    private String templateType = "LADDER";

    /** 首重重量(kg)，首重续重类型使用 */
    private BigDecimal firstWeight;

    /** 首重费用(元)，首重续重类型使用 */
    private BigDecimal firstFee;

    /** 续重重量(kg)，首重续重类型使用 */
    private BigDecimal additionalWeight;

    /** 续重费用(元/kg)，首重续重类型使用 */
    private BigDecimal additionalFee;

    /** 是否默认模板 */
    private Integer isDefault = 0;

    /** 状态：1-启用 0-禁用 */
    private Integer status = 1;

    /** 备注 */
    private String remark;

    /** 费用阶梯列表（阶梯计费类型使用） */
    @Valid
    private List<FeeStepDTO> steps;

    /**
     * 费用阶梯项
     */
    @Data
    public static class FeeStepDTO {

        /** 阶梯ID（修改时使用） */
        private Long id;

        /** 最小重量(kg) */
        @NotNull(message = "最小重量不能为空")
        private BigDecimal minWeight;

        /** 最大重量(kg) */
        @NotNull(message = "最大重量不能为空")
        private BigDecimal maxWeight;

        /** 费用(元) */
        @NotNull(message = "费用不能为空")
        private BigDecimal fee;

        /** 排序 */
        private Integer sortOrder = 0;
    }
}
