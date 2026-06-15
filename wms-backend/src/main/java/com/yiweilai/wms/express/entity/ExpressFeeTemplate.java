package com.yiweilai.wms.express.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 快递费用模板实体
 */
@Data
public class ExpressFeeTemplate {

    /** 模板ID */
    private Long id;

    /** 快递公司ID */
    private Long companyId;

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

    /** 逻辑删除 */
    private Integer deleted;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 关联的费用阶梯（非数据库字段，阶梯类型使用） */
    private List<ExpressFeeStep> steps;
}
