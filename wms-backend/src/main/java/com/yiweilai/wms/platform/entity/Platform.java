package com.yiweilai.wms.platform.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 平台实体
 */
@Data
public class Platform {

    /** 平台ID */
    private Long id;

    /** 平台名称 */
    private String name;

    /** 展示颜色（十六进制） */
    private String color;

    /** 是否启用：1-启用 0-禁用 */
    private Integer enabled;

    /** 备注 */
    private String remark;

    /** 逻辑删除：0-未删 1-已删 */
    private Integer deleted;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
