package com.yiweilai.wms.system.controller;

import com.yiweilai.wms.common.Result;
import com.yiweilai.wms.log.annotation.OperationLog;
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
    @OperationLog(module = "system", action = "backup_full", targetType = "Backup")
    @PostMapping("/full")
    public Result<BackupRecord> fullBackup(@RequestBody(required = false) Map<String, String> body) {
        String backupPath = body != null ? body.get("backupPath") : null;
        return Result.success(backupService.fullBackup(backupPath));
    }

    /**
     * 增量备份
     */
    @OperationLog(module = "system", action = "backup_incremental", targetType = "Backup")
    @PostMapping("/incremental")
    public Result<BackupRecord> incrementalBackup(@RequestBody(required = false) Map<String, String> body) {
        String backupPath = body != null ? body.get("backupPath") : null;
        return Result.success(backupService.incrementalBackup(backupPath));
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
    @OperationLog(module = "system", action = "backup_delete", targetType = "Backup", targetIdParam = "id")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        backupService.deleteBackup(id);
        return Result.success();
    }

    /**
     * 测试远程备份连接
     */
    @PostMapping("/test-connection")
    public Result<Boolean> testConnection(@RequestBody Map<String, String> config) {
        String type = config.getOrDefault("type", "");
        String host = config.getOrDefault("host", "");
        String port = config.getOrDefault("port", "22");
        String username = config.getOrDefault("username", "");
        String password = config.getOrDefault("password", "");

        try {
            boolean success = switch (type) {
                case "ssh", "sftp" -> testSshConnection(host, Integer.parseInt(port), username, password);
                case "ftp" -> testFtpConnection(host, Integer.parseInt(port), username, password);
                default -> false;
            };
            return Result.success(success);
        } catch (Exception e) {
            return Result.success(false);
        }
    }

    private boolean testSshConnection(String host, int port, String username, String password) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "ssh", "-o", "ConnectTimeout=5", "-o", "StrictHostKeyChecking=no",
                    "-o", "BatchMode=yes",
                    "-p", String.valueOf(port),
                    username + "@" + host,
                    "echo ok"
            );
            pb.environment().put("SSHPASS", password);
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean testFtpConnection(String host, int port, String username, String password) {
        try {
            java.net.Socket socket = new java.net.Socket();
            socket.connect(new java.net.InetSocketAddress(host, port), 5000);
            socket.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
