package com.yiweilai.wms.system.controller;

import com.yiweilai.wms.common.Result;
import com.yiweilai.wms.system.entity.BackupRecord;
import com.yiweilai.wms.system.service.BackupService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 数据库备份 Controller
 */
@RestController
@RequestMapping("/api/backup")
@RequiredArgsConstructor
public class BackupController {

    private final BackupService backupService;

    /**
     * 全量备份
     */
    @PostMapping("/full")
    public Result<BackupRecord> fullBackup(@RequestBody(required = false) Map<String, String> body) {
        String backupPath = body != null ? body.get("backupPath") : null;
        return Result.success(backupService.fullBackup(backupPath));
    }

    /**
     * 增量备份
     */
    @PostMapping("/incremental")
    public Result<BackupRecord> incrementalBackup(@RequestBody(required = false) Map<String, Object> body) {
        Long baseBackupId = null;
        String backupPath = null;
        if (body != null) {
            if (body.get("baseBackupId") != null) {
                baseBackupId = Long.valueOf(body.get("baseBackupId").toString());
            }
            backupPath = (String) body.get("backupPath");
        }
        return Result.success(backupService.incrementalBackup(baseBackupId, backupPath));
    }

    /**
     * 查询备份记录
     */
    @GetMapping("/list")
    public Result<List<BackupRecord>> list() {
        return Result.success(backupService.getBackupList());
    }

    /**
     * 下载备份文件
     */
    @GetMapping("/download/{fileName}")
    public void download(@PathVariable String fileName, HttpServletResponse response) throws IOException {
        File file = backupService.getBackupFile(fileName);

        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition",
                "attachment; filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
        response.setContentLengthLong(file.length());

        try (InputStream is = new FileInputStream(file);
             OutputStream os = response.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
        }
    }

    /**
     * 删除备份记录
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        backupService.deleteBackup(id);
        return Result.success();
    }
}
