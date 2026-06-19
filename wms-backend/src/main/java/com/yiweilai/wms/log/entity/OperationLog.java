package com.yiweilai.wms.log.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 操作日志实体
 */
@Data
public class OperationLog {

    /** 日志ID */
    private Long id;

    /** 操作人ID */
    private Long userId;

    /** 操作人姓名 */
    private String userName;

    /** 模块 */
    private String module;

    /** 操作 */
    private String action;

    /** 操作对象类型 */
    private String targetType;

    /** 操作对象ID */
    private Long targetId;

    /** 操作详情 */
    private String detail;

    /** IP地址 */
    private String ip;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
