package com.yiweilai.wms.ocr.controller;

import com.yiweilai.wms.common.Result;
import com.yiweilai.wms.ocr.dto.ExpressOcrResult;
import com.yiweilai.wms.ocr.dto.GeneralOcrResult;
import com.yiweilai.wms.ocr.dto.OcrRequest;
import com.yiweilai.wms.ocr.service.OcrService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * OCR识别控制器
 */
@Tag(name = "OCR识别", description = "快递单识别、文字识别")
@RestController
@RequestMapping("/api/ocr")
@RequiredArgsConstructor
public class OcrController {

    private final OcrService ocrService;

    @Operation(summary = "快递单识别（图片Base64）")
    @PostMapping("/express")
    public Result<ExpressOcrResult> recognizeExpress(@RequestBody OcrRequest request) {
        ExpressOcrResult result = ocrService.recognizeExpress(request);
        return Result.success(result);
    }

    @Operation(summary = "快递单识别（文件上传）")
    @PostMapping("/express/upload")
    public Result<ExpressOcrResult> recognizeExpressUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "baidu") String engine) {
        ExpressOcrResult result = ocrService.recognizeExpress(file, engine);
        return Result.success(result);
    }

    @Operation(summary = "通用文字识别（图片Base64）")
    @PostMapping("/general")
    public Result<GeneralOcrResult> recognizeGeneral(@RequestBody OcrRequest request) {
        GeneralOcrResult result = ocrService.recognizeGeneral(request);
        return Result.success(result);
    }

    @Operation(summary = "通用文字识别（文件上传）")
    @PostMapping("/general/upload")
    public Result<GeneralOcrResult> recognizeGeneralUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "baidu") String engine) {
        GeneralOcrResult result = ocrService.recognizeGeneral(file, engine);
        return Result.success(result);
    }
}
