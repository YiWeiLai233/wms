package com.yiweilai.wms.returns.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量创建退货单参数
 */
@Data
public class ReturnBatchCreateDTO {

    /** 订单ID列表 */
    @NotEmpty(message = "订单ID列表不能为空")
    private List<Long> orderIds;

    /** 退货原因 */
    @NotBlank(message = "退货原因不能为空")
    private String reason;

    /** 客户快递单号 */
    private String trackingNo;

    /** 备注 */
    private String remark;
}
