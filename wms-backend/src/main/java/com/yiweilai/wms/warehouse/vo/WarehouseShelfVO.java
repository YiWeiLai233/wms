package com.yiweilai.wms.warehouse.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 货架返回对象
 */
@Data
public class WarehouseShelfVO {

    /** 货架ID */
    private Long id;

    /** 库区ID */
    private Long areaId;

    /** 货架编码 */
    private String code;

    /** 货架名称 */
    private String name;

    /** 商品分类 */
    private String categoryName;

    /** 状态：1-启用 0-禁用 */
    private Integer status;

    /** 库位列表 */
    private List<WarehouseLocationVO> locationList;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
