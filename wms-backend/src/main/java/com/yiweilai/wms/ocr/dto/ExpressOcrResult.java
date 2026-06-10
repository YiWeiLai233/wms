package com.yiweilai.wms.ocr.dto;

import lombok.Data;

import java.util.List;

/**
 * 快递单OCR识别结果
 */
@Data
public class ExpressOcrResult {

    /** 快递单号 */
    private String trackingNo;

    /** 快递公司 */
    private String expressCompany;

    /** 收件人姓名 */
    private String receiverName;

    /** 收件人电话 */
    private String receiverPhone;

    /** 收件人地址 */
    private String receiverAddress;

    /** 寄件人姓名 */
    private String senderName;

    /** 寄件人电话 */
    private String senderPhone;

    /** 寄件人地址 */
    private String senderAddress;

    /** 原始识别文本 */
    private String rawText;

    /** 识别置信度 */
    private Double confidence;

    /** 识别到的所有文本行 */
    private List<TextLine> textLines;

    /**
     * 文本行
     */
    @Data
    public static class TextLine {
        /** 文本内容 */
        private String text;
        /** 置信度 */
        private Double confidence;
        /** 位置坐标 */
        private List<Integer> position;
    }
}
