package com.yiweilai.wms.report.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ExpressFeeUpdateDTO {

    @NotNull(message = "请选择快递公司")
    private Long expressCompanyId;

    @NotNull(message = "请输入快递费用")
    @DecimalMin(value = "0.00", message = "快递费用不能小于0")
    private BigDecimal shippingFee;
}
