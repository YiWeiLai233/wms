package com.yiweilai.wms.log.dto;

import lombok.Data;

/**
 * 操作日志查询参数
 */
@Data
public class OperationLogQueryDTO {

    /** 页码（从1开始） */
    private Integer page = 1;

    /** 每页数量 */
    private Integer size = 10;

    /** 操作人ID */
    private Long userId;

    /** 操作类型 */
    private String operation;

    /** 操作状态 */
    private Integer status;
}
