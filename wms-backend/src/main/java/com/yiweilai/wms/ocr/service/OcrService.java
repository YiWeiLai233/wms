package com.yiweilai.wms.ocr.service;

import com.yiweilai.wms.ocr.dto.ExpressOcrResult;
import com.yiweilai.wms.ocr.dto.GeneralOcrResult;
import com.yiweilai.wms.ocr.dto.OcrRequest;
import org.springframework.web.multipart.MultipartFile;

/**
 * OCR服务接口
 */
public interface OcrService {

    /**
     * 快递单识别
     */
    ExpressOcrResult recognizeExpress(OcrRequest request);

    /**
     * 快递单识别（文件上传）
     */
    ExpressOcrResult recognizeExpress(MultipartFile file, String engine);

    /**
     * 通用文字识别
     */
    GeneralOcrResult recognizeGeneral(OcrRequest request);

    /**
     * 通用文字识别（文件上传）
     */
    GeneralOcrResult recognizeGeneral(MultipartFile file, String engine);
}
