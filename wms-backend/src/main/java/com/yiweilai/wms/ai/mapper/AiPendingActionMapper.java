package com.yiweilai.wms.ai.mapper;

import com.yiweilai.wms.ai.entity.AiPendingAction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiPendingActionMapper {

    int insert(AiPendingAction action);

    AiPendingAction findById(@Param("id") Long id);

    List<AiPendingAction> findPendingByUserId(@Param("userId") Long userId);

    int markConfirmed(@Param("id") Long id);

    int markExecuted(@Param("id") Long id, @Param("resultData") String resultData);

    int markCancelled(@Param("id") Long id);

    int markExpired(@Param("id") Long id);

    int markFailed(@Param("id") Long id, @Param("errorMessage") String errorMessage);
}
