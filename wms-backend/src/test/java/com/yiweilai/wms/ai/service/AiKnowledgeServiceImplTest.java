package com.yiweilai.wms.ai.service;

import com.yiweilai.wms.ai.client.AiServiceClient;
import com.yiweilai.wms.ai.dto.AiKnowledgeIngestRequest;
import com.yiweilai.wms.ai.entity.AiKnowledgeDocument;
import com.yiweilai.wms.ai.mapper.AiKnowledgeDocumentMapper;
import com.yiweilai.wms.ai.service.impl.AiKnowledgeServiceImpl;
import com.yiweilai.wms.file.service.FileService;
import com.yiweilai.wms.file.vo.FileVO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiKnowledgeServiceImplTest {

    @Test
    void uploadStoresKnowledgeDocumentAndRequestsIngestion() {
        FileService fileService = mock(FileService.class);
        AiKnowledgeDocumentMapper documentMapper = mock(AiKnowledgeDocumentMapper.class);
        AiServiceClient aiServiceClient = mock(AiServiceClient.class);

        FileVO file = new FileVO();
        file.setId(12L);
        file.setFileName("入库流程.docx");
        file.setFilePath("/uploads/2026/06/14/abc.docx");
        file.setFileType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        file.setFileSize(1024L);
        when(fileService.upload(any(), eq("AI_KNOWLEDGE"), eq(null))).thenReturn(file);
        when(documentMapper.insert(any(AiKnowledgeDocument.class))).thenAnswer(invocation -> {
            AiKnowledgeDocument document = invocation.getArgument(0);
            document.setId(33L);
            return 1;
        });

        AiKnowledgeServiceImpl service = new AiKnowledgeServiceImpl(fileService, documentMapper, aiServiceClient);
        MockMultipartFile upload = new MockMultipartFile("file", "入库流程.docx", file.getFileType(), "hello".getBytes());

        AiKnowledgeDocument document = service.upload(5L, upload);

        assertThat(document.getId()).isEqualTo(33L);
        assertThat(document.getStatus()).isEqualTo("PROCESSING");
        assertThat(document.getCreatedBy()).isEqualTo(5L);
        verify(aiServiceClient).ingestKnowledge(org.mockito.ArgumentMatchers.argThat((AiKnowledgeIngestRequest request) ->
                Long.valueOf(33L).equals(request.getDocumentId())
                        && "/uploads/2026/06/14/abc.docx".equals(request.getFilePath())
                        && "入库流程.docx".equals(request.getFileName())));
    }
}
