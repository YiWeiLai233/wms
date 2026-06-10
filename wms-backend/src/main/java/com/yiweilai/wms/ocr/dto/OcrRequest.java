package com.yiweilai.wms.ocr.dto;

import lombok.Data;

/**
 * OCR请求参数
 */
@Data
public class OcrRequest {

    /** 图片URL或Base64 */
    private String imageUrl;

    /** 图片Base64编码（与imageUrl二选一） */
    private String imageBase64;

    /** OCR引擎：baidu/tesseract，默认baidu */
    private String engine = "baidu";

    /** 识别类型：express-快递单/general-通用文字，默认express */
    private String type = "express";
}
