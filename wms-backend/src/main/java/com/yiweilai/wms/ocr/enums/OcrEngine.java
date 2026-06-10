package com.yiweilai.wms.ocr.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * OCR引擎枚举
 */
@Getter
@AllArgsConstructor
public enum OcrEngine {

    /** 百度OCR */
    BAIDU("baidu", "百度OCR"),

    /** Tesseract本地OCR */
    TESSERACT("tesseract", "Tesseract本地OCR");

    /** 引擎编码 */
    private final String code;

    /** 引擎名称 */
    private final String name;

    /**
     * 根据编码获取枚举
     */
    public static OcrEngine fromCode(String code) {
        for (OcrEngine engine : values()) {
            if (engine.getCode().equals(code)) {
                return engine;
            }
        }
        return BAIDU; // 默认使用百度
    }
}
