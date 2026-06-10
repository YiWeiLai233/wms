package com.yiweilai.wms.ocr.dto;

import lombok.Data;

import java.util.List;

/**
 * 通用文字OCR识别结果
 */
@Data
public class GeneralOcrResult {

    /** 识别到的全部文本 */
    private String fullText;

    /** 文本行列表 */
    private List<TextBlock> textBlocks;

    /** 语言 */
    private String language;

    /**
     * 文本块
     */
    @Data
    public static class TextBlock {
        /** 文本内容 */
        private String text;
        /** 置信度 */
        private Double confidence;
        /** 位置坐标 */
        private List<Integer> position;
    }
}
