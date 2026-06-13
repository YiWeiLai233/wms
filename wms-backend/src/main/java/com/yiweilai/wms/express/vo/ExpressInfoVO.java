package com.yiweilai.wms.express.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 快递信息返回对象
 */
@Data
public class ExpressInfoVO {

    /** 快递单号 */
    private String trackingNo;

    /** 快递公司编码 */
    private String carrier;

    /** 快递公司名称 */
    private String carrierName;

    /** 快递状态 */
    private String status;

    /** 状态描述 */
    private String statusDesc;

    /** 物流轨迹 */
    private List<Track> tracks;

    /** 费用信息 */
    private FeeInfo feeInfo;

    /**
     * 物流轨迹
     */
    @Data
    public static class Track {
        /** 时间 */
        private String time;
        /** 内容 */
        private String content;
        /** 地点 */
        private String location;
    }

    /**
     * 费用信息
     */
    @Data
    public static class FeeInfo {
        /** 总重量 */
        private BigDecimal totalWeight;
        /** 首重 */
        private BigDecimal firstWeight;
        /** 首重费用 */
        private BigDecimal firstWeightFee;
        /** 续重费用 */
        private BigDecimal additionalWeightFee;
        /** 总费用 */
        private BigDecimal totalFee;
        /** 计费方式 */
        private String billingMethod;
    }
}
