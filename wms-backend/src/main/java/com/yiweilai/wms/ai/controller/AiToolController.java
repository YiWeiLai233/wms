package com.yiweilai.wms.ai.controller;

import com.yiweilai.wms.ai.dto.tool.AiOrderQueryRequest;
import com.yiweilai.wms.ai.dto.tool.AiOrderSearchRequest;
import com.yiweilai.wms.ai.dto.tool.AiOutboundQueryRequest;
import com.yiweilai.wms.ai.dto.tool.AiReturnQueryRequest;
import com.yiweilai.wms.ai.dto.tool.AiStockQueryRequest;
import com.yiweilai.wms.ai.dto.tool.AiToolContext;
import com.yiweilai.wms.ai.dto.tool.AiToolResult;
import com.yiweilai.wms.ai.tool.AiOrderToolService;
import com.yiweilai.wms.ai.tool.AiOutboundToolService;
import com.yiweilai.wms.ai.tool.AiReturnToolService;
import com.yiweilai.wms.ai.tool.AiStockToolService;
import com.yiweilai.wms.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/tools")
@RequiredArgsConstructor
public class AiToolController {

    private final AiOrderToolService orderToolService;
    private final AiStockToolService stockToolService;
    private final AiOutboundToolService outboundToolService;
    private final AiReturnToolService returnToolService;

    @PostMapping("/order/query")
    public Result<AiToolResult<?>> queryOrderByNo(@RequestBody(required = false) AiOrderQueryRequest request,
                                                  @RequestHeader(value = "X-AI-User-Id", required = false) Long userId,
                                                  @RequestHeader(value = "X-AI-Conversation-Id", required = false) Long conversationId,
                                                  @RequestHeader(value = "X-AI-Message-Id", required = false) Long messageId) {
        return Result.success(orderToolService.queryOrderByNo(request, AiToolContext.of(userId, conversationId, messageId)));
    }

    @PostMapping("/order/search")
    public Result<AiToolResult<?>> searchOrders(@RequestBody(required = false) AiOrderSearchRequest request,
                                                @RequestHeader(value = "X-AI-User-Id", required = false) Long userId,
                                                @RequestHeader(value = "X-AI-Conversation-Id", required = false) Long conversationId,
                                                @RequestHeader(value = "X-AI-Message-Id", required = false) Long messageId) {
        return Result.success(orderToolService.searchOrders(request, AiToolContext.of(userId, conversationId, messageId)));
    }

    @PostMapping("/order/count-pending-outbound")
    public Result<AiToolResult<?>> countPendingOutboundOrders(
            @RequestHeader(value = "X-AI-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-AI-Conversation-Id", required = false) Long conversationId,
            @RequestHeader(value = "X-AI-Message-Id", required = false) Long messageId) {
        return Result.success(orderToolService.countPendingOutboundOrders(AiToolContext.of(userId, conversationId, messageId)));
    }

    @PostMapping("/stock/query")
    public Result<AiToolResult<?>> querySkuInventory(@RequestBody(required = false) AiStockQueryRequest request,
                                                     @RequestHeader(value = "X-AI-User-Id", required = false) Long userId,
                                                     @RequestHeader(value = "X-AI-Conversation-Id", required = false) Long conversationId,
                                                     @RequestHeader(value = "X-AI-Message-Id", required = false) Long messageId) {
        return Result.success(stockToolService.querySkuInventory(request, AiToolContext.of(userId, conversationId, messageId)));
    }

    @PostMapping("/outbound/query")
    public Result<AiToolResult<?>> queryOutboundOrders(@RequestBody(required = false) AiOutboundQueryRequest request,
                                                       @RequestHeader(value = "X-AI-User-Id", required = false) Long userId,
                                                       @RequestHeader(value = "X-AI-Conversation-Id", required = false) Long conversationId,
                                                       @RequestHeader(value = "X-AI-Message-Id", required = false) Long messageId) {
        return Result.success(outboundToolService.queryOutboundOrders(request, AiToolContext.of(userId, conversationId, messageId)));
    }

    @PostMapping("/return/query")
    public Result<AiToolResult<?>> queryReturnOrders(@RequestBody(required = false) AiReturnQueryRequest request,
                                                     @RequestHeader(value = "X-AI-User-Id", required = false) Long userId,
                                                     @RequestHeader(value = "X-AI-Conversation-Id", required = false) Long conversationId,
                                                     @RequestHeader(value = "X-AI-Message-Id", required = false) Long messageId) {
        return Result.success(returnToolService.queryReturnOrders(request, AiToolContext.of(userId, conversationId, messageId)));
    }
}
