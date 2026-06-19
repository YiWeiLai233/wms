package com.yiweilai.wms.log.aspect;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yiweilai.wms.log.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Parameter;
import java.util.*;

/**
 * 操作日志 AOP 切面
 * 拦截 @OperationLog 注解的方法，自动生成人类可读的操作日志
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogService operationLogService;
    private final ObjectMapper objectMapper;

    /** 操作描述模板：模块 → action → 中文描述 */
    private static final Map<String, Map<String, String>> ACTION_LABELS = Map.ofEntries(
            // 订单
            Map.entry("order", Map.of(
                    "import", "导入订单",
                    "import_file", "文件批量导入订单",
                    "update", "修改订单",
                    "update_status", "更新订单状态"
            )),
            // 商品
            Map.entry("product", Map.of(
                    "create", "新增商品",
                    "update", "修改商品",
                    "delete", "删除商品",
                    "sku_create", "新增SKU",
                    "sku_update", "修改SKU",
                    "sku_delete", "删除SKU"
            )),
            // 库存
            Map.entry("stock", Map.of(
                    "adjust", "调整库存",
                    "batch_adjust", "批量入库",
                    "confirm_sellable", "确认可售（次品→普通仓）",
                    "confirm_dispose", "确认报废处置",
                    "confirm_scrap", "转入报废仓"
            )),
            // 出库
            Map.entry("outbound", Map.of(
                    "create", "创建出库单",
                    "create_batch", "批量创建出库单",
                    "confirm", "确认出库",
                    "update", "修改出库单",
                    "cancel", "取消出库单"
            )),
            // 退货
            Map.entry("return", Map.of(
                    "create", "创建退货单",
                    "create_batch", "批量创建退货单",
                    "check", "退货质检",
                    "confirm", "确认退货入库",
                    "cancel", "取消退货单",
                    "import_file", "文件批量导入退货单"
            )),
            // 换货
            Map.entry("exchange", Map.of(
                    "create", "创建换货单",
                    "receive", "确认收货",
                    "check", "换货质检",
                    "ship", "换货发货",
                    "cancel", "取消换货单"
            )),
            // 仓库
            Map.entry("warehouse", Map.of(
                    "create", "新增仓库",
                    "update", "修改仓库",
                    "delete", "删除仓库"
            )),
            // 快递
            Map.entry("express", Map.of(
                    "create", "新增快递公司",
                    "update", "修改快递公司",
                    "delete", "删除快递公司"
            )),
            // 用户
            Map.entry("user", Map.of(
                    "create", "新增用户",
                    "update", "修改用户",
                    "delete", "删除用户",
                    "reset_password", "重置密码"
            )),
            // 系统
            Map.entry("system", Map.of(
                    "backup_full", "全量备份",
                    "backup_incremental", "增量备份",
                    "backup_delete", "删除备份"
            )),
            // AI助手
            Map.entry("ai_assistant", Map.of(
                    "CREATE_OUTBOUND", "AI创建出库单",
                    "CONFIRM_OUTBOUND", "AI确认出库",
                    "CREATE_RETURN", "AI创建退货单",
                    "CHECK_RETURN", "AI退货质检",
                    "CONFIRM_RETURN", "AI确认退货入库",
                    "ADJUST_STOCK", "AI调整库存"
            ))
    );

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint,
                         com.yiweilai.wms.log.annotation.OperationLog operationLog) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = null;
        Throwable caughtException = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            caughtException = e;
            throw e;
        } finally {
            try {
                recordLog(joinPoint, operationLog, result, caughtException, startTime);
            } catch (Exception e) {
                log.warn("记录操作日志失败: {}", e.getMessage());
            }
        }
    }

    private void recordLog(ProceedingJoinPoint joinPoint,
                           com.yiweilai.wms.log.annotation.OperationLog annotation,
                           Object result, Throwable exception, long startTime) {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return;
        }

        com.yiweilai.wms.log.entity.OperationLog logEntity =
                new com.yiweilai.wms.log.entity.OperationLog();

        // 用户信息
        Object userId = request.getAttribute("userId");
        Object username = request.getAttribute("username");
        if (userId instanceof Long) {
            logEntity.setUserId((Long) userId);
        }
        if (username instanceof String) {
            logEntity.setUserName((String) username);
        }

        // 模块和操作
        logEntity.setModule(annotation.module());
        logEntity.setAction(annotation.action());

        // 操作对象类型
        if (StringUtils.hasText(annotation.targetType())) {
            logEntity.setTargetType(annotation.targetType());
        }

        // 操作对象ID
        Long targetId = extractTargetId(joinPoint, annotation.targetIdParam());
        if (targetId != null) {
            logEntity.setTargetId(targetId);
        }

        // 生成人类可读的操作描述
        long duration = System.currentTimeMillis() - startTime;
        String description = buildDescription(annotation, joinPoint, result, exception, duration);
        logEntity.setDetail(description);

        // IP
        logEntity.setIp(getClientIp(request));

        // 异步写入
        operationLogService.saveLog(logEntity);
    }

    /**
     * 生成人类可读的操作描述
     */
    private String buildDescription(com.yiweilai.wms.log.annotation.OperationLog annotation,
                                    ProceedingJoinPoint joinPoint,
                                    Object result, Throwable exception, long duration) {
        StringBuilder sb = new StringBuilder();

        // 操作名称
        String actionLabel = getActionLabel(annotation.module(), annotation.action());
        sb.append(actionLabel);

        // 提取关键参数信息
        String paramSummary = extractParamSummary(joinPoint);
        if (StringUtils.hasText(paramSummary)) {
            sb.append("，").append(paramSummary);
        }

        // 结果
        if (exception != null) {
            sb.append("。❌ 失败：").append(getSimpleErrorMessage(exception));
        } else {
            sb.append("。✅ 成功");
        }

        // 耗时
        if (duration > 1000) {
            sb.append("，耗时 ").append(String.format("%.1f", duration / 1000.0)).append("s");
        } else {
            sb.append("，耗时 ").append(duration).append("ms");
        }

        return sb.toString();
    }

    /**
     * 获取操作的中文标签
     */
    private String getActionLabel(String module, String action) {
        Map<String, String> moduleActions = ACTION_LABELS.get(module);
        if (moduleActions != null) {
            String label = moduleActions.get(action);
            if (label != null) {
                return label;
            }
        }
        return module + "." + action;
    }

    /**
     * 提取关键参数摘要（人类可读）
     */
    private String extractParamSummary(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        List<String> parts = new ArrayList<>();

        for (int i = 0; i < paramNames.length; i++) {
            String name = paramNames[i];
            Object arg = args[i];

            // 跳过不可序列化的类型
            if (arg instanceof HttpServletRequest
                    || arg instanceof jakarta.servlet.http.HttpServletResponse
                    || arg instanceof jakarta.servlet.http.HttpSession) {
                continue;
            }

            // 跳过 MultipartFile
            if (arg instanceof org.springframework.web.multipart.MultipartFile) {
                parts.add("文件: " + ((org.springframework.web.multipart.MultipartFile) arg).getOriginalFilename());
                continue;
            }

            // 简单类型直接显示
            if (arg instanceof String || arg instanceof Number || arg instanceof Boolean) {
                parts.add(name + "=" + arg);
                continue;
            }

            // DTO 对象：提取关键字段
            try {
                JsonNode node = objectMapper.valueToTree(arg);
                String dtoSummary = extractDtoSummary(node, name);
                if (StringUtils.hasText(dtoSummary)) {
                    parts.add(dtoSummary);
                }
            } catch (Exception ignored) {
            }
        }

        return String.join("，", parts);
    }

    /**
     * 从 DTO JSON 中提取关键字段摘要
     */
    private String extractDtoSummary(JsonNode node, String paramName) {
        if (node == null || !node.isObject()) {
            return null;
        }

        List<String> items = new ArrayList<>();

        // 优先提取的字段（按重要性排序）
        String[] priorityFields = {
                "id", "platformOrderNo", "orderNo", "skuCode", "skuName", "name",
                "receiverName", "receiverPhone", "receiverAddress",
                "reason", "trackingNo", "quantity",
                "warehouseId", "platformId", "orderId", "returnId",
                "remark", "status"
        };

        for (String field : priorityFields) {
            JsonNode value = node.get(field);
            if (value != null && !value.isNull() && !value.isMissingNode()) {
                String displayValue = value.asText();
                if (StringUtils.hasText(displayValue)) {
                    items.add(getFieldLabel(field) + "=" + displayValue);
                }
            }
        }

        // 如果有 items 数组，显示详情
        JsonNode itemsNode = node.get("items");
        if (itemsNode != null && itemsNode.isArray() && itemsNode.size() > 0) {
            List<String> itemDetails = new ArrayList<>();
            for (int j = 0; j < itemsNode.size(); j++) {
                JsonNode itemNode = itemsNode.get(j);
                if (itemNode.isObject()) {
                    List<String> itemParts = new ArrayList<>();
                    JsonNode skuCodeNode = itemNode.get("skuCode");
                    if (skuCodeNode != null && !skuCodeNode.isNull()) {
                        itemParts.add(skuCodeNode.asText());
                    }
                    JsonNode skuNameNode = itemNode.get("skuName");
                    if (skuNameNode != null && !skuNameNode.isNull()) {
                        itemParts.add(skuNameNode.asText());
                    }
                    JsonNode qtyNode = itemNode.get("quantity");
                    if (qtyNode != null && !qtyNode.isNull()) {
                        itemParts.add("×" + qtyNode.asText());
                    }
                    JsonNode sizeNode = itemNode.get("sizeValue");
                    if (sizeNode != null && !sizeNode.isNull() && StringUtils.hasText(sizeNode.asText())) {
                        itemParts.add(sizeNode.asText() + "码");
                    }
                    if (!itemParts.isEmpty()) {
                        itemDetails.add(String.join(" ", itemParts));
                    }
                }
            }
            if (!itemDetails.isEmpty()) {
                items.add("明细: " + String.join("; ", itemDetails));
            } else {
                items.add("共" + itemsNode.size() + "项");
            }
        }

        return items.isEmpty() ? null : String.join("，", items);
    }

    /**
     * 字段名转中文标签
     */
    private String getFieldLabel(String field) {
        return switch (field) {
            case "id" -> "ID";
            case "platformOrderNo" -> "平台单号";
            case "orderNo" -> "订单号";
            case "skuCode" -> "SKU编码";
            case "skuName" -> "商品名称";
            case "name" -> "名称";
            case "receiverName" -> "收件人";
            case "receiverPhone" -> "收件人电话";
            case "receiverAddress" -> "收件地址";
            case "reason" -> "原因";
            case "trackingNo" -> "快递单号";
            case "quantity" -> "数量";
            case "warehouseId" -> "仓库ID";
            case "platformId" -> "平台ID";
            case "orderId" -> "订单ID";
            case "returnId" -> "退货单ID";
            case "remark" -> "备注";
            case "status" -> "状态";
            default -> field;
        };
    }

    /**
     * 提取完整错误信息
     */
    private String getSimpleErrorMessage(Throwable exception) {
        String msg = exception.getMessage();
        if (!StringUtils.hasText(msg)) {
            msg = exception.getClass().getSimpleName();
        }
        return msg;
    }

    /**
     * 从方法参数中提取 targetId
     */
    private Long extractTargetId(ProceedingJoinPoint joinPoint, String paramName) {
        if (!StringUtils.hasText(paramName)) {
            return null;
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Parameter[] parameters = signature.getMethod().getParameters();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < parameters.length; i++) {
            PathVariable pathVar = parameters[i].getAnnotation(PathVariable.class);
            if (pathVar != null && paramName.equals(pathVar.value())) {
                return toLong(args[i]);
            }
            RequestParam reqParam = parameters[i].getAnnotation(RequestParam.class);
            if (reqParam != null && paramName.equals(reqParam.value())) {
                return toLong(args[i]);
            }
            if (paramName.equals(parameters[i].getName())) {
                return toLong(args[i]);
            }
        }
        return null;
    }

    private Long toLong(Object value) {
        if (value instanceof Long) return (Long) value;
        if (value instanceof Number) return ((Number) value).longValue();
        if (value instanceof String) {
            try { return Long.parseLong((String) value); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }

    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (StringUtils.hasText(ip) && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
