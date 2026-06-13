package com.yiweilai.wms.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 错误码枚举
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    // 通用
    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    // 用户模块 1xxx
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_PASSWORD_ERROR(1002, "密码错误"),
    USER_DISABLED(1003, "用户已禁用"),
    USER_EXISTS(1004, "用户已存在"),

    // 商品模块 2xxx
    PRODUCT_NOT_FOUND(2001, "商品不存在"),
    SKU_NOT_FOUND(2002, "SKU不存在"),
    BARCODE_EXISTS(2003, "条码已存在"),

    // 仓库模块 3xxx
    WAREHOUSE_NOT_FOUND(3001, "仓库不存在"),

    // 库存模块 4xxx
    STOCK_NOT_ENOUGH(4001, "库存不足"),
    STOCK_LOCKED(4002, "库存已锁定"),
    STOCK_CHECK_RUNNING(4003, "盘点进行中，不能调整"),

    // 订单模块 5xxx
    ORDER_NOT_FOUND(5001, "订单不存在"),
    ORDER_STATUS_ERROR(5002, "订单状态不正确"),

    // 出库模块 6xxx
    OUTBOUND_NOT_FOUND(6001, "出库单不存在"),
    OUTBOUND_STATUS_ERROR(6002, "出库单状态不正确，不能出库"),
    OUTBOUND_ALREADY_SHIPPED(6003, "出库单已发货，不能重复操作"),

    // 退货模块 7xxx
    RETURN_NOT_FOUND(7001, "退货单不存在"),
    RETURN_STATUS_ERROR(7002, "退货单状态不正确"),

    // ES 同步模块 8xxx
    ES_SYNC_FAILED(8001, "ES同步失败");

    private final Integer code;
    private final String message;
}
