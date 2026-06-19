package com.yiweilai.wms.common.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 图片访问 Controller - 兼容旧URL格式 /images/{fileName}
 */
@Slf4j
@RestController
public class ImageServeController {

    @Value("${image.storage.path:./uploads/images}")
    private String storagePath;

    @GetMapping("/images/{fileName}")
    public void serve(@PathVariable String fileName, HttpServletResponse response) throws IOException {
        Path filePath = Paths.get(storagePath, fileName);
        if (!Files.exists(filePath)) {
            response.setStatus(404);
            response.getWriter().write("图片不存在");
            return;
        }

        String ext = getFileExtension(fileName).toLowerCase();
        String contentType;
        switch (ext) {
            case "jpg":
            case "jpeg":
                contentType = "image/jpeg";
                break;
            case "png":
                contentType = "image/png";
                break;
            case "gif":
                contentType = "image/gif";
                break;
            case "webp":
                contentType = "image/webp";
                break;
            default:
                contentType = "application/octet-stream";
        }
        response.setContentType(contentType);
        response.setHeader("Cache-Control", "public, max-age=2592000");
        Files.copy(filePath, response.getOutputStream());
    }

    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf(".");
        if (lastDotIndex == -1) {
            return "";
        }
        return fileName.substring(lastDotIndex + 1);
    }
}
