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

    /** 明细列表 */
    private List<ExpressFeeItem> items;

    @Data
    public static class ExpressFeeItem {
        private Long outboundId;
        private String outboundNo;
        private String orderNo;
        private Long expressCompanyId;
        private String expressCompanyName;
        private String trackingNo;
        private BigDecimal shippingFee;
        private String shippedAt;
    }
}
