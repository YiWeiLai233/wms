package com.yiweilai.wms.warehouse.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 库区返回对象
 */
@Data
public class WarehouseAreaVO {

    /** 库区ID */
    private Long id;

    /** 仓库ID */
    private Long warehouseId;

    /** 库区编码 */
    private String code;

    /** 库区名称 */
    private String name;

    /** 类型：1-普通区 2-退货区 3-次品区 */
    private Integer type;

    /** 状态：1-启用 0-禁用 */
    private Integer status;

    /** 货架列表 */
    private List<WarehouseShelfVO> shelfList;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
