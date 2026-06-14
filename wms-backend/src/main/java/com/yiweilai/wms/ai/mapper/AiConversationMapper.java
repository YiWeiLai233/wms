package com.yiweilai.wms.ai.mapper;

import com.yiweilai.wms.ai.entity.AiConversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiConversationMapper {

    int insert(AiConversation conversation);

    AiConversation findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    List<AiConversation> findByUserId(@Param("userId") Long userId);

    int touch(@Param("id") Long id);

    int deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}
