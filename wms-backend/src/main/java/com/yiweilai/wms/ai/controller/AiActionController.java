package com.yiweilai.wms.ai.controller;

import com.yiweilai.wms.ai.dto.action.AiActionExecuteResult;
import com.yiweilai.wms.ai.dto.action.AiConfirmActionRequest;
import com.yiweilai.wms.ai.dto.action.AiCreateActionRequest;
import com.yiweilai.wms.ai.dto.action.AiPendingActionVO;
import com.yiweilai.wms.ai.service.AiPendingActionService;
import com.yiweilai.wms.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import com.yiweilai.wms.security.RequirePermission;

@RequirePermission("ai.assistant")
@RestController
@RequestMapping("/api/ai/actions")
@RequiredArgsConstructor
public class AiActionController {

    private final AiPendingActionService pendingActionService;

    @PostMapping("/pending")
    public Result<AiPendingActionVO> createPendingAction(@RequestBody AiCreateActionRequest request) {
        return Result.success(pendingActionService.createPendingAction(request));
    }

    @GetMapping("/pending")
    public Result<List<AiPendingActionVO>> listPendingActions(@RequestAttribute("userId") Long userId) {
        return Result.success(pendingActionService.listPendingActions(userId));
    }

    @GetMapping("/{actionId}")
    public Result<AiPendingActionVO> getAction(@PathVariable Long actionId,
                                               @RequestAttribute("userId") Long userId) {
        return Result.success(pendingActionService.getAction(actionId, userId));
    }

    @PostMapping("/{actionId}/confirm")
    public Result<AiActionExecuteResult> confirm(@PathVariable Long actionId,
                                                 @RequestBody(required = false) AiConfirmActionRequest request,
                                                 @RequestAttribute("userId") Long userId,
                                                 @RequestAttribute("username") String username,
                                                 @RequestAttribute(value = "roles", required = false) List<String> roles) {
        return Result.success(pendingActionService.confirmAndExecute(actionId, userId, username, roles));
    }

    @PostMapping("/{actionId}/cancel")
    public Result<Void> cancel(@PathVariable Long actionId,
                               @RequestAttribute("userId") Long userId) {
        pendingActionService.cancel(actionId, userId);
        return Result.success();
    }
}
