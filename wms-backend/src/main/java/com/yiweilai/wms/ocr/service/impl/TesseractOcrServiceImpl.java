package com.yiweilai.wms.ocr.service.impl;

import com.yiweilai.wms.ocr.dto.GeneralOcrResult;
import com.yiweilai.wms.ocr.service.OcrEngineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Tesseract本地OCR引擎实现
 * 注意：需要安装Tesseract OCR并配置环境变量
 */
@Slf4j
@Service("tesseractOcrService")
public class TesseractOcrServiceImpl implements OcrEngineService {

    @Override
    public String getEngineName() {
        return "tesseract";
    }

    @Override
    public GeneralOcrResult recognize(String imageBase64) {
        GeneralOcrResult result = new GeneralOcrResult();
        result.setTextBlocks(new ArrayList<>());

        try {
            // 解码Base64图片
            byte[] imageBytes = Base64.getDecoder().decode(imageBase64);
            ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes);
            BufferedImage image = ImageIO.read(bis);

            if (image == null) {
                result.setFullText("无法读取图片");
                return result;
            }

            // 使用Tesseract进行OCR识别
            // 注意：这里需要tess4j依赖，或者调用系统命令
            String text = doOcr(imageBytes);
            result.setFullText(text);

            // 简单按行分割
            List<GeneralOcrResult.TextBlock> textBlocks = new ArrayList<>();
            String[] lines = text.split("\n");
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    GeneralOcrResult.TextBlock block = new GeneralOcrResult.TextBlock();
                    block.setText(line.trim());
                    block.setConfidence(0.8);
                    textBlocks.add(block);
                }
            }
            result.setTextBlocks(textBlocks);
            result.setLanguage("chi_sim+eng");

        } catch (Exception e) {
            log.error("Tesseract OCR识别失败", e);
            result.setFullText("OCR识别失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 执行OCR识别
     * 方式1：使用tess4j库（推荐）
     * 方式2：调用系统tesseract命令
     */
    private String doOcr(byte[] imageBytes) {
        // 方式2：调用系统tesseract命令
        try {
            // 保存临时文件
            java.io.File tempFile = java.io.File.createTempFile("ocr_", ".png");
            java.nio.file.Files.write(tempFile.toPath(), imageBytes);

            // 构建tesseract命令
            ProcessBuilder pb = new ProcessBuilder(
                    "tesseract",
                    tempFile.getAbsolutePath(),
                    "stdout",
                    "-l", "chi_sim+eng",
                    "--psm", "6"
            );
            pb.redirectErrorStream(true);

            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes());
            process.waitFor();

            // 删除临时文件
            tempFile.delete();

            return output.trim();
        } catch (Exception e) {
            log.error("Tesseract命令执行失败，请确保已安装Tesseract并配置环境变量", e);
            return "Tesseract未安装或配置错误。请安装Tesseract OCR并配置环境变量。下载地址：https://github.com/tesseract-ocr/tesseract";
        }
    }
}
