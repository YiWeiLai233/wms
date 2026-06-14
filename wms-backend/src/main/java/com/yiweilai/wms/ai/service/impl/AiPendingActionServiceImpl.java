package com.yiweilai.wms.ai.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yiweilai.wms.ai.dto.action.AiActionExecuteResult;
import com.yiweilai.wms.ai.dto.action.AiCreateActionRequest;
import com.yiweilai.wms.ai.dto.action.AiPendingActionVO;
import com.yiweilai.wms.ai.entity.AiPendingAction;
import com.yiweilai.wms.ai.mapper.AiPendingActionMapper;
import com.yiweilai.wms.ai.service.AiPendingActionService;
import com.yiweilai.wms.ai.tool.AiWriteToolService;
import com.yiweilai.wms.exception.BusinessException;
import com.yiweilai.wms.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AiPendingActionServiceImpl implements AiPendingActionService {

    private static final Set<String> SUPPORTED_ACTIONS = Set.of(
            "create_outbound_order",
            "confirm_outbound_order",
            "create_return_order",
            "check_return_order",
            "confirm_return_order",
            "adjust_stock");

    private static final Map<String, String> DEFAULT_RISK_LEVELS = Map.of(
            "create_outbound_order", "MEDIUM",
            "confirm_outbound_order", "HIGH",
            "create_return_order", "MEDIUM",
            "check_return_order", "MEDIUM",
            "confirm_return_order", "HIGH",
            "adjust_stock", "HIGH");

    private final AiPendingActionMapper pendingActionMapper;
    private final AiWriteToolService writeToolService;
    private final ObjectMapper objectMapper;

    @Override
    public AiPendingActionVO createPendingAction(AiCreateActionRequest request) {
        validateCreateRequest(request);
        AiPendingAction action = new AiPendingAction();
        action.setUserId(request.getUserId());
        action.setConversationId(request.getConversationId());
        action.setActionType(request.getActionType().trim());
        action.setActionName(StringUtils.hasText(request.getActionName())
                ? request.getActionName().trim()
                : request.getActionType().trim());
        action.setRequestParams(toJson(request.getRequestParams()));
        action.setSummary(request.getSummary());
        action.setRiskLevel(resolveRiskLevel(request));
        action.setStatus("PENDING");
        action.setExpireAt(LocalDateTime.now().plusMinutes(30));
        pendingActionMapper.insert(action);
        return toVO(action);
    }

    @Override
    public AiPendingActionVO getAction(Long actionId, Long userId) {
        return toVO(requireOwnedAction(actionId, userId));
    }

    @Override
    public List<AiPendingActionVO> listPendingActions(Long userId) {
        return pendingActionMapper.findPendingByUserId(userId).stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public AiActionExecuteResult confirmAndExecute(Long actionId, Long userId, String username, List<String> roles) {
        AiPendingAction action = requireOwnedAction(actionId, userId);
        validateExecutable(action, roles);
        pendingActionMapper.markConfirmed(actionId);
        try {
            AiActionExecuteResult result = writeToolService.execute(action, userId, username, roles);
            pendingActionMapper.markExecuted(actionId, toJson(result.getResultData()));
            return result;
        } catch (RuntimeException ex) {
            String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            pendingActionMapper.markFailed(actionId, message);
            throw ex;
        }
    }

    @Override
    public void cancel(Long actionId, Long userId) {
        AiPendingAction action = requireOwnedAction(actionId, userId);
        if (!"PENDING".equals(action.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "只有待确认操作可以取消");
        }
        pendingActionMapper.markCancelled(actionId);
    }

    private void validateCreateRequest(AiCreateActionRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请求不能为空");
        }
        if (request.getUserId() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "userId 不能为空");
        }
        if (!StringUtils.hasText(request.getActionType()) || !SUPPORTED_ACTIONS.contains(request.getActionType().trim())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的 AI 写操作");
        }
        if (request.getRequestParams() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "requestParams 不能为空");
        }
    }

    private String resolveRiskLevel(AiCreateActionRequest request) {
        if (StringUtils.hasText(request.getRiskLevel())) {
            return request.getRiskLevel().trim().toUpperCase();
        }
        return DEFAULT_RISK_LEVELS.getOrDefault(request.getActionType().trim(), "MEDIUM");
    }

    private AiPendingAction requireOwnedAction(Long actionId, Long userId) {
        if (actionId == null || userId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "actionId 和 userId 不能为空");
        }
        AiPendingAction action = pendingActionMapper.findById(actionId);
        if (action == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "待确认操作不存在");
        }
        if (!userId.equals(action.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该待确认操作");
        }
        return action;
    }

    private void validateExecutable(AiPendingAction action, List<String> roles) {
        if (!"PENDING".equals(action.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "当前操作状态不允许执行");
        }
        if (action.getExpireAt() == null || action.getExpireAt().isBefore(LocalDateTime.now())) {
            pendingActionMapper.markExpired(action.getId());
            throw new BusinessException(ErrorCode.BAD_REQUEST, "待确认操作已过期");
        }
        if (!hasPermission(action, roles)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权执行该 AI 写操作");
        }
    }

    private boolean hasPermission(AiPendingAction action, List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return false;
        }
        Set<String> normalizedRoles = roles.stream()
                .filter(StringUtils::hasText)
                .map(role -> role.startsWith("ROLE_") ? role.substring("ROLE_".length()) : role)
                .collect(java.util.stream.Collectors.toSet());
        if (normalizedRoles.contains("SUPER_ADMIN")) {
            return true;
        }
        if (normalizedRoles.contains("VIEWER")) {
            return false;
        }
        if (normalizedRoles.contains("WAREHOUSE_ADMIN")) {
            return true;
        }
        if (normalizedRoles.contains("OPERATOR")) {
            return !"HIGH".equalsIgnoreCase(action.getRiskLevel())
                    && !"adjust_stock".equals(action.getActionType());
        }
        return false;
    }

    private AiPendingActionVO toVO(AiPendingAction action) {
        AiPendingActionVO vo = new AiPendingActionVO();
        vo.setActionId(action.getId());
        vo.setUserId(action.getUserId());
        vo.setConversationId(action.getConversationId());
        vo.setActionType(action.getActionType());
        vo.setActionName(action.getActionName());
        vo.setRequestParams(parseJson(action.getRequestParams()));
        vo.setSummary(action.getSummary());
        vo.setRiskLevel(action.getRiskLevel());
        vo.setStatus(action.getStatus());
        vo.setNeedConfirm("PENDING".equals(action.getStatus()));
        vo.setExpireAt(action.getExpireAt());
        vo.setConfirmedAt(action.getConfirmedAt());
        vo.setExecutedAt(action.getExecutedAt());
        vo.setResultData(parseJson(action.getResultData()));
        vo.setErrorMessage(action.getErrorMessage());
        vo.setCreatedAt(action.getCreatedAt());
        vo.setUpdatedAt(action.getUpdatedAt());
        return vo;
    }

    private Object parseJson(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return json;
        }
    }

    private String toJson(Object value) {
        if (value == null) {
            return "{}";
        }
        if (value instanceof String text) {
            return text;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "JSON 序列化失败");
        }
    }
}
