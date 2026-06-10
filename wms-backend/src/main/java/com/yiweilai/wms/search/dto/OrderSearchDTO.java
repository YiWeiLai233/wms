package com.yiweilai.wms.search.dto;

import lombok.Data;

/**
 * 订单搜索参数
 */
@Data
public class OrderSearchDTO {

    /** 页码（从1开始） */
    private Integer page = 1;

    /** 每页数量 */
    private Integer size = 10;

    /** 搜索关键词（订单号/收件人/电话/商品名） */
    private String keyword;

    /** 订单状态 */
    private String orderStatus;

    /** 仓库ID */
    private Long warehouseId;

    /** 开始时间 */
    private String startTime;

    /** 结束时间 */
    private String endTime;
}
