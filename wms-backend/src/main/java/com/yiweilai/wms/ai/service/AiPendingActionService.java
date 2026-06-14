package com.yiweilai.wms.ai.service;

import com.yiweilai.wms.ai.dto.action.AiActionExecuteResult;
import com.yiweilai.wms.ai.dto.action.AiCreateActionRequest;
import com.yiweilai.wms.ai.dto.action.AiPendingActionVO;

import java.util.List;

public interface AiPendingActionService {

    AiPendingActionVO createPendingAction(AiCreateActionRequest request);

    AiPendingActionVO getAction(Long actionId, Long userId);

    List<AiPendingActionVO> listPendingActions(Long userId);

    AiActionExecuteResult confirmAndExecute(Long actionId, Long userId, String username, List<String> roles);

    void cancel(Long actionId, Long userId);
}
