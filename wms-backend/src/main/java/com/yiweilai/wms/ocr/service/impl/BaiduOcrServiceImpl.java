package com.yiweilai.wms.ocr.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yiweilai.wms.ocr.dto.GeneralOcrResult;
import com.yiweilai.wms.ocr.service.OcrEngineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * 百度OCR引擎实现
 */
@Slf4j
@Service("baiduOcrService")
public class BaiduOcrServiceImpl implements OcrEngineService {

    @Value("${ocr.baidu.api-key:}")
    private String apiKey;

    @Value("${ocr.baidu.secret-key:}")
    private String secretKey;

    @Value("${ocr.baidu.enabled:false}")
    private boolean enabled;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String TOKEN_URL = "https://aip.baidubce.com/oauth/2.0/token";
    private static final String OCR_URL = "https://aip.baidubce.com/rest/2.0/ocr/v1/general_basic";

    @Override
    public String getEngineName() {
        return "baidu";
    }

    @Override
    public GeneralOcrResult recognize(String imageBase64) {
        GeneralOcrResult result = new GeneralOcrResult();
        result.setTextBlocks(new ArrayList<>());

        if (!enabled) {
            log.warn("百度OCR未启用，请配置 ocr.baidu.enabled=true 和 API Key");
            result.setFullText("百度OCR未配置，请在application.yml中配置ocr.baidu相关参数");
            return result;
        }

        try {
            // 获取Access Token
            String accessToken = getAccessToken();
            if (accessToken == null) {
                result.setFullText("获取百度OCR Token失败");
                return result;
            }

            // 调用OCR接口
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("image", imageBase64);
            params.add("language_type", "CHN_ENG");

            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(params, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    OCR_URL + "?access_token=" + accessToken,
                    requestEntity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode wordsResult = root.path("words_result");

                StringBuilder fullText = new StringBuilder();
                List<GeneralOcrResult.TextBlock> textBlocks = new ArrayList<>();

                if (wordsResult.isArray()) {
                    for (JsonNode item : wordsResult) {
                        String words = item.path("words").asText();
                        fullText.append(words).append("\n");

                        GeneralOcrResult.TextBlock block = new GeneralOcrResult.TextBlock();
                        block.setText(words);
                        block.setConfidence(0.9);
                        textBlocks.add(block);
                    }
                }

                result.setFullText(fullText.toString().trim());
                result.setTextBlocks(textBlocks);
                result.setLanguage("CHN_ENG");
            }
        } catch (Exception e) {
            log.error("百度OCR识别失败", e);
            result.setFullText("OCR识别失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 获取百度Access Token
     */
    private String getAccessToken() {
        try {
            String url = TOKEN_URL +
                    "?grant_type=client_credentials" +
                    "&client_id=" + apiKey +
                    "&client_secret=" + secretKey;

            ResponseEntity<String> response = restTemplate.postForEntity(url, null, String.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                return root.path("access_token").asText();
            }
        } catch (Exception e) {
            log.error("获取百度OCR Token失败", e);
        }
        return null;
    }
}
