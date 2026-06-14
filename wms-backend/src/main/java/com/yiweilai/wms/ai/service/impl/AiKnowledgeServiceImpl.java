package com.yiweilai.wms.ai.service.impl;

import com.yiweilai.wms.ai.client.AiServiceClient;
import com.yiweilai.wms.ai.dto.AiKnowledgeChunkDTO;
import com.yiweilai.wms.ai.dto.AiKnowledgeIngestRequest;
import com.yiweilai.wms.ai.dto.AiKnowledgeIngestResult;
import com.yiweilai.wms.ai.entity.AiKnowledgeChunk;
import com.yiweilai.wms.ai.entity.AiKnowledgeDocument;
import com.yiweilai.wms.ai.mapper.AiKnowledgeChunkMapper;
import com.yiweilai.wms.ai.mapper.AiKnowledgeDocumentMapper;
import com.yiweilai.wms.ai.service.AiKnowledgeService;
import com.yiweilai.wms.exception.BusinessException;
import com.yiweilai.wms.exception.ErrorCode;
import com.yiweilai.wms.file.service.FileService;
import com.yiweilai.wms.file.vo.FileVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
public class AiKnowledgeServiceImpl implements AiKnowledgeService {

    private final FileService fileService;
    private final AiKnowledgeDocumentMapper documentMapper;
    private final AiServiceClient aiServiceClient;
    private final AiKnowledgeChunkMapper chunkMapper;

    @Autowired
    public AiKnowledgeServiceImpl(FileService fileService,
                                  AiKnowledgeDocumentMapper documentMapper,
                                  AiServiceClient aiServiceClient,
                                  AiKnowledgeChunkMapper chunkMapper) {
        this.fileService = fileService;
        this.documentMapper = documentMapper;
        this.aiServiceClient = aiServiceClient;
        this.chunkMapper = chunkMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiKnowledgeDocument upload(Long userId, MultipartFile file) {
        FileVO fileVO = fileService.upload(file, "AI_KNOWLEDGE", null);

        AiKnowledgeDocument document = new AiKnowledgeDocument();
        document.setTitle(fileVO.getFileName());
        document.setFileId(fileVO.getId());
        document.setFileName(fileVO.getFileName());
        document.setFilePath(fileVO.getFilePath());
        document.setSourceType("FILE");
        document.setStatus("PROCESSING");
        document.setChunkCount(0);
        document.setCreatedBy(userId);
        documentMapper.insert(document);

        requestIngestion(document);
        return document;
    }

    @Override
    public List<AiKnowledgeDocument> list(Long userId) {
        return documentMapper.findByUserId(userId);
    }

    @Override
    public AiKnowledgeDocument getById(Long userId, Long id) {
        AiKnowledgeDocument document = documentMapper.findByIdAndUserId(id, userId);
        if (document == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "知识库文档不存在");
        }
        return document;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long id) {
        AiKnowledgeDocument document = getById(userId, id);
        if (chunkMapper != null) {
            chunkMapper.deleteByDocumentId(id);
        }
        documentMapper.deleteByIdAndUserId(id, userId);
        try {
            aiServiceClient.deleteKnowledge(document.getId());
        } catch (RuntimeException e) {
            log.warn("AI服务删除知识文档失败: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiKnowledgeDocument rebuild(Long userId, Long id) {
        AiKnowledgeDocument document = getById(userId, id);
        document.setStatus("PROCESSING");
        document.setErrorMessage(null);
        document.setChunkCount(0);
        documentMapper.updateStatus(document);
        if (chunkMapper != null) {
            chunkMapper.deleteByDocumentId(id);
        }
        requestIngestion(document);
        return document;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeIngestion(Long documentId, AiKnowledgeIngestResult result) {
        AiKnowledgeDocument document = documentMapper.findById(documentId);
        if (document == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "知识库文档不存在");
        }

        if (!"SUCCESS".equalsIgnoreCase(result.getStatus())) {
            document.setStatus("FAILED");
            document.setChunkCount(0);
            document.setErrorMessage(result.getErrorMessage());
            documentMapper.updateIngestionResult(document);
            return;
        }

        if (chunkMapper != null) {
            chunkMapper.deleteByDocumentId(documentId);
            for (AiKnowledgeChunkDTO chunkDTO : result.getChunks()) {
                AiKnowledgeChunk chunk = new AiKnowledgeChunk();
                chunk.setDocumentId(documentId);
                chunk.setChunkIndex(chunkDTO.getChunkIndex());
                chunk.setContent(chunkDTO.getContent());
                chunk.setVectorId(chunkDTO.getVectorId());
                chunk.setMetadata(chunkDTO.getMetadata());
                chunkMapper.insert(chunk);
            }
        }
        document.setStatus("SUCCESS");
        document.setChunkCount(result.getChunks() == null ? 0 : result.getChunks().size());
        document.setErrorMessage(null);
        documentMapper.updateIngestionResult(document);
    }

    private void requestIngestion(AiKnowledgeDocument document) {
        try {
            aiServiceClient.ingestKnowledge(AiKnowledgeIngestRequest.builder()
                    .documentId(document.getId())
                    .title(document.getTitle())
                    .fileName(document.getFileName())
                    .filePath(document.getFilePath())
                    .build());
        } catch (RuntimeException e) {
            log.warn("AI服务知识库索引失败: {}", e.getMessage());
            document.setStatus("FAILED");
            document.setErrorMessage(e.getMessage());
            documentMapper.updateStatus(document);
        }
    }
}
