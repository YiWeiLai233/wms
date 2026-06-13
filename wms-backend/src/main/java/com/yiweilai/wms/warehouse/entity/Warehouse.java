package com.yiweilai.wms.warehouse.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 仓库实体
 */
@Data
public class Warehouse {

    /** 仓库ID */
    private Long id;

    /** 仓库编码 */
    private String code;

    /** 仓库名称 */
    private String name;

    /** 仓库地址 */
    private String address;

    /** 联系人 */
    private String contact;

    /** 联系电话 */
    private String phone;

    /** 状态：1-启用 0-禁用 */
    private Integer status;

    /** 仓库类型：NORMAL-普通仓 DEFECTIVE-次品仓 SCRAP-报废仓 */
    private String warehouseType;

    /** 逻辑删除 */
    private Integer deleted;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
