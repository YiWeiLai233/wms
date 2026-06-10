package com.yiweilai.wms.ocr.service.impl;

import com.yiweilai.wms.exception.BusinessException;
import com.yiweilai.wms.exception.ErrorCode;
import com.yiweilai.wms.ocr.dto.ExpressOcrResult;
import com.yiweilai.wms.ocr.dto.GeneralOcrResult;
import com.yiweilai.wms.ocr.dto.OcrRequest;
import com.yiweilai.wms.ocr.service.OcrEngineService;
import com.yiweilai.wms.ocr.service.OcrService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OCR服务实现
 */
@Slf4j
@Service
public class OcrServiceImpl implements OcrService {

    private final Map<String, OcrEngineService> engineMap = new HashMap<>();

    public OcrServiceImpl(
            @Qualifier("baiduOcrService") OcrEngineService baiduOcrService,
            @Qualifier("tesseractOcrService") OcrEngineService tesseractOcrService) {
        engineMap.put("baidu", baiduOcrService);
        engineMap.put("tesseract", tesseractOcrService);
    }

    @Override
    public ExpressOcrResult recognizeExpress(OcrRequest request) {
        // 获取图片Base64
        String imageBase64 = getImageBase64(request);

        // 获取OCR引擎
        OcrEngineService engine = getEngine(request.getEngine());

        // 执行OCR识别
        GeneralOcrResult generalResult = engine.recognize(imageBase64);

        // 解析快递单信息
        return parseExpressInfo(generalResult);
    }

    @Override
    public ExpressOcrResult recognizeExpress(MultipartFile file, String engineName) {
        try {
            String imageBase64 = Base64.getEncoder().encodeToString(file.getBytes());
            OcrRequest request = new OcrRequest();
            request.setImageBase64(imageBase64);
            request.setEngine(engineName);
            return recognizeExpress(request);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "图片读取失败");
        }
    }

    @Override
    public GeneralOcrResult recognizeGeneral(OcrRequest request) {
        String imageBase64 = getImageBase64(request);
        OcrEngineService engine = getEngine(request.getEngine());
        return engine.recognize(imageBase64);
    }

    @Override
    public GeneralOcrResult recognizeGeneral(MultipartFile file, String engineName) {
        try {
            String imageBase64 = Base64.getEncoder().encodeToString(file.getBytes());
            OcrRequest request = new OcrRequest();
            request.setImageBase64(imageBase64);
            request.setEngine(engineName);
            return recognizeGeneral(request);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "图片读取失败");
        }
    }

    /**
     * 获取图片Base64
     */
    private String getImageBase64(OcrRequest request) {
        if (request.getImageBase64() != null && !request.getImageBase64().isEmpty()) {
            return request.getImageBase64();
        }
        // TODO: 如果是URL，需要下载图片并转为Base64
        throw new BusinessException(ErrorCode.BAD_REQUEST, "请提供图片Base64或URL");
    }

    /**
     * 获取OCR引擎
     */
    private OcrEngineService getEngine(String engineName) {
        OcrEngineService engine = engineMap.get(engineName);
        if (engine == null) {
            engine = engineMap.get("baidu"); // 默认使用百度
        }
        return engine;
    }

    /**
     * 解析快递单信息
     */
    private ExpressOcrResult parseExpressInfo(GeneralOcrResult generalResult) {
        ExpressOcrResult result = new ExpressOcrResult();
        result.setRawText(generalResult.getFullText());

        String text = generalResult.getFullText();
        if (text == null || text.isEmpty()) {
            return result;
        }

        // 提取快递单号（常见格式：12-15位数字）
        result.setTrackingNo(extractTrackingNo(text));

        // 提取手机号（11位手机号）
        result.setReceiverPhone(extractPhone(text));

        // 提取地址（包含省市区的行）
        result.setReceiverAddress(extractAddress(text));

        // 提取姓名（电话号码前面的内容）
        result.setReceiverName(extractName(text));

        // 识别快递公司
        result.setExpressCompany(extractExpressCompany(text));

        return result;
    }

    /**
     * 提取快递单号
     */
    private String extractTrackingNo(String text) {
        // 常见快递单号格式
        String[] patterns = {
                "\\b(\\d{12,15})\\b",  // 12-15位纯数字
                "\\b([A-Z]{2}\\d{9}[A-Z]{2})\\b",  // 国际快递格式
                "单号[：:]?\\s*(\\w{10,20})",  // 单号：xxx
                "运单[：:]?\\s*(\\w{10,20})",  // 运单：xxx
                "快递[：:]?\\s*(\\w{10,20})",  // 快递：xxx
        };

        for (String pattern : patterns) {
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(text);
            if (m.find()) {
                return m.group(1);
            }
        }
        return null;
    }

    /**
     * 提取手机号
     */
    private String extractPhone(String text) {
        Pattern p = Pattern.compile("\\b(1[3-9]\\d{9})\\b");
        Matcher m = p.matcher(text);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    /**
     * 提取地址
     */
    private String extractAddress(String text) {
        // 包含省市区的行
        String[] patterns = {
                ".*?[省市区县].*?[路街道巷号楼].*",
                "收件地址[：:]?\\s*(.+)",
                "地址[：:]?\\s*(.+)",
        };

        for (String pattern : patterns) {
            Pattern p = Pattern.compile(pattern, Pattern.MULTILINE);
            Matcher m = p.matcher(text);
            if (m.find()) {
                return m.group(1) != null ? m.group(1).trim() : m.group(0).trim();
            }
        }
        return null;
    }

    /**
     * 提取姓名
     */
    private String extractName(String text) {
        String[] patterns = {
                "收件人[：:]?\\s*(\\S+)",
                "收件[：:]?\\s*(\\S+)",
                "姓名[：:]?\\s*(\\S+)",
        };

        for (String pattern : patterns) {
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(text);
            if (m.find()) {
                return m.group(1).trim();
            }
        }
        return null;
    }

    /**
     * 识别快递公司
     */
    private String extractExpressCompany(String text) {
        Map<String, String> companyMap = new HashMap<>();
        companyMap.put("顺丰", "顺丰速运");
        companyMap.put("SF", "顺丰速运");
        companyMap.put("圆通", "圆通速递");
        companyMap.put("YTO", "圆通速递");
        companyMap.put("中通", "中通快递");
        companyMap.put("ZTO", "中通快递");
        companyMap.put("韵达", "韵达快递");
        companyMap.put("YD", "韵达快递");
        companyMap.put("申通", "申通快递");
        companyMap.put("STO", "申通快递");
        companyMap.put("百世", "百世快递");
        companyMap.put("极兔", "极兔速递");
        companyMap.put("京东", "京东物流");
        companyMap.put("EMS", "EMS");
        companyMap.put("邮政", "中国邮政");

        String upperText = text.toUpperCase();
        for (Map.Entry<String, String> entry : companyMap.entrySet()) {
            if (upperText.contains(entry.getKey().toUpperCase())) {
                return entry.getValue();
            }
        }
        return null;
    }
}
