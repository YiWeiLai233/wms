package com.yiweilai.wms.warehouse.dto;

import lombok.Data;

/**
 * 仓库查询参数
 */
@Data
public class WarehouseQueryDTO {

    /** 页码（从1开始） */
    private Integer page = 1;

    /** 每页数量 */
    private Integer size = 10;

    /** 关键词（名称/编码） */
    private String keyword;

    /** 状态 */
    private Integer status;
}
