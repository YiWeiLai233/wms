package com.yiweilai.wms.ai.mapper;

import com.yiweilai.wms.ai.entity.AiKnowledgeChunk;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiKnowledgeChunkMapper {

    int insert(AiKnowledgeChunk chunk);

    List<AiKnowledgeChunk> findByDocumentId(@Param("documentId") Long documentId);

    int deleteByDocumentId(@Param("documentId") Long documentId);
}
