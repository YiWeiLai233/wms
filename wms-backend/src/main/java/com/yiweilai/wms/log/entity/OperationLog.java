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

    /** 操作类型 */
    private String operation;

    /** 方法名 */
    private String method;

    /** 请求参数 */
    private String params;

    /** 返回结果 */
    private String result;

    /** 操作状态：0-失败 1-成功 */
    private Integer status;

    /** 错误信息 */
    private String errorMsg;

    /** 操作IP */
    private String ip;

    /** 操作时间 */
    private LocalDateTime createdAt;
}
