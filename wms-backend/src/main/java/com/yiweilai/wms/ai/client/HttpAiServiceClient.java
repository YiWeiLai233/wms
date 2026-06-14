package com.yiweilai.wms.ai.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yiweilai.wms.ai.dto.AiChatResponse;
import com.yiweilai.wms.ai.dto.AiKnowledgeIngestRequest;
import com.yiweilai.wms.ai.dto.AiServiceChatRequest;
import com.yiweilai.wms.exception.BusinessException;
import com.yiweilai.wms.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class HttpAiServiceClient implements AiServiceClient {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Value("${ai.service.base-url:http://localhost:8010}")
    private String baseUrl;

    @Value("${ai.service.token:}")
    private String token;

    @Override
    public AiChatResponse chat(AiServiceChatRequest request) {
        return post("/api/chat", request, AiChatResponse.class);
    }

    @Override
    public void ingestKnowledge(AiKnowledgeIngestRequest request) {
        post("/api/knowledge/ingest", request, Void.class);
    }

    @Override
    public void deleteKnowledge(Long documentId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(normalizedBaseUrl() + "/api/knowledge/documents/" + documentId))
                    .timeout(Duration.ofSeconds(15))
                    .header("X-AI-Service-Token", token)
                    .DELETE()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AI服务删除知识文档失败");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AI服务不可用: " + e.getMessage());
        }
    }

    private <T> T post(String path, Object body, Class<T> responseType) {
        try {
            String requestBody = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(normalizedBaseUrl() + path))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .header("X-AI-Service-Token", token)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AI服务调用失败: HTTP " + response.statusCode());
            }
            if (Void.class.equals(responseType)) {
                return null;
            }
            return objectMapper.readValue(response.body(), responseType);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AI服务不可用: " + e.getMessage());
        }
    }

    private String normalizedBaseUrl() {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
