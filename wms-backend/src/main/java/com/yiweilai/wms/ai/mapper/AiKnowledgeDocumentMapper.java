package com.yiweilai.wms.ai.mapper;

import com.yiweilai.wms.ai.entity.AiKnowledgeDocument;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiKnowledgeDocumentMapper {

    int insert(AiKnowledgeDocument document);

    AiKnowledgeDocument findById(@Param("id") Long id);

    AiKnowledgeDocument findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    List<AiKnowledgeDocument> findByUserId(@Param("userId") Long userId);

    int updateStatus(AiKnowledgeDocument document);

    int updateIngestionResult(AiKnowledgeDocument document);

    int deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}
