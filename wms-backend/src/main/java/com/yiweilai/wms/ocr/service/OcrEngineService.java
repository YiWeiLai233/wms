package com.yiweilai.wms.ocr.service;

import com.yiweilai.wms.ocr.dto.GeneralOcrResult;

/**
 * OCR引擎服务接口（不同引擎实现）
 */
public interface OcrEngineService {

    /**
     * 获取引擎名称
     */
    String getEngineName();

    /**
     * 识别图片文字
     *
     * @param imageBase64 图片Base64编码
     * @return 识别结果
     */
    GeneralOcrResult recognize(String imageBase64);
}
