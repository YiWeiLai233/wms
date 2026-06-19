package com.yiweilai.wms.common.controller;

import com.yiweilai.wms.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 图片上传 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/images")
public class ImageController {

    @Value("${image.storage.path:./uploads/images}")
    private String storagePath;

    @Value("${image.base-url:}")
    private String configuredBaseUrl;

    @Value("${image.max-size:5MB}")
    private String maxSize;

    @Value("${image.allowed-types:jpg,jpeg,png,gif,webp}")
    private String allowedTypes;

    /**
     * 上传图片
     */
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        // 检查文件是否为空
        if (file.isEmpty()) {
            return Result.error(400, "请选择要上传的文件");
        }

        // 检查文件类型
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return Result.error(400, "文件名不能为空");
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!isAllowedType(extension)) {
            return Result.error(400, "不支持的文件类型，允许的类型：" + allowedTypes);
        }

        // 生成唯一文件名
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;

        try {
            // 创建存储目录
            Path uploadPath = Paths.get(storagePath);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 保存文件
            Path filePath = uploadPath.resolve(fileName);
            file.transferTo(filePath.toFile());

            // 返回访问URL：优先使用配置的base-url，否则返回相对路径
            String url;
            if (configuredBaseUrl != null && !configuredBaseUrl.isEmpty()) {
                url = configuredBaseUrl + "/" + fileName;
            } else {
                // 返回相对路径，由nginx代理访问
                url = "/api/images/file/" + fileName;
            }
            log.info("图片上传成功: {}", url);
            return Result.success(url);
        } catch (IOException e) {
            log.error("图片上传失败", e);
            return Result.error(500, "图片上传失败");
        }
    }

    /**
     * 访问已上传的图片
     */
    @GetMapping("/file/{fileName}")
    public void getFile(@PathVariable String fileName, jakarta.servlet.http.HttpServletResponse response) throws IOException {
        Path filePath = Paths.get(storagePath, fileName);
        if (!Files.exists(filePath)) {
            response.setStatus(404);
            response.getWriter().write("图片不存在");
            return;
        }
        // 根据扩展名设置Content-Type
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
        response.setHeader("Cache-Control", "public, max-age=2592000"); // 缓存30天
        Files.copy(filePath, response.getOutputStream());
    }

    /**
     * 删除图片
     */
    @DeleteMapping("/delete")
    public Result<Void> delete(@RequestParam("url") String url) {
        try {
            // 从URL中提取文件名
            String fileName = url.substring(url.lastIndexOf("/") + 1);
            Path filePath = Paths.get(storagePath, fileName);

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("图片删除成功: {}", url);
            }
            return Result.success();
        } catch (IOException e) {
            log.error("图片删除失败", e);
            return Result.error(500, "图片删除失败");
        }
    }

    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf(".");
        if (lastDotIndex == -1) {
            return "";
        }
        return fileName.substring(lastDotIndex + 1);
    }

    private boolean isAllowedType(String extension) {
        String[] types = allowedTypes.split(",");
        for (String type : types) {
            if (type.trim().equalsIgnoreCase(extension)) {
                return true;
            }
        }
        return false;
    }
}
