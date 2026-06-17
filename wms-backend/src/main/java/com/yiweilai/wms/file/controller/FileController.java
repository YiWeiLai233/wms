package com.yiweilai.wms.file.controller;

import com.yiweilai.wms.common.Result;
import com.yiweilai.wms.file.service.FileService;
import com.yiweilai.wms.file.vo.FileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.yiweilai.wms.security.RequirePermission;

/**
 * 文件 Controller
 */
@RequirePermission("system.files")
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    /**
     * 上传文件
     */
    @PostMapping("/upload")
    public Result<FileVO> upload(@RequestParam("file") MultipartFile file,
                                 @RequestParam(value = "bizType", required = false) String bizType,
                                 @RequestParam(value = "bizId", required = false) Long bizId) {
        return Result.success(fileService.upload(file, bizType, bizId));
    }

    /**
     * 获取文件信息
     */
    @GetMapping("/{id}")
    public Result<FileVO> getById(@PathVariable Long id) {
        return Result.success(fileService.getById(id));
    }

    /**
     * 删除文件
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        fileService.delete(id);
        return Result.success();
    }
}
