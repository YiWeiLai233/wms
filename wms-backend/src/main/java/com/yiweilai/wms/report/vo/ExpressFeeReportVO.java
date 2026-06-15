package com.yiweilai.wms.report.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 快递费用统计
 */
@Data
public class ExpressFeeReportVO {

    /** 总费用 */
    private BigDecimal totalFee;

    /** 总单数 */
    private Long totalCount;

    /** 出库快递费用 */
    private BigDecimal outboundFee;

    /** 出库快递单数 */
    private Long outboundCount;

    /** 退货快递费用 */
    private BigDecimal returnFee;

    /** 退货快递单数 */
    private Long returnCount;

    /** 明细列表 */
    private List<ExpressFeeItem> items;

    @Data
    public static class ExpressFeeItem {
        private Long id;
        private String bizNo;
        private String orderNo;
        private String platformOrderNo;
        private String bizType; // OUTBOUND 或 RETURN
        private String bizTypeName; // 出库 或 退货
        private Long expressCompanyId;
        private String expressCompanyName;
        private String trackingNo;
        private BigDecimal shippingFee;
        private String createdAt;
    }
}
