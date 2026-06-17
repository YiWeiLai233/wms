package com.yiweilai.wms.system.service;

import com.yiweilai.wms.system.entity.BackupConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

/**
 * 远程备份服务 - 使用 SCP 上传备份文件
 */
@Slf4j
@Service
public class RemoteBackupService {

    /**
     * 上传备份文件到远程服务器
     */
    public boolean uploadToRemote(File file, BackupConfig config) {
        if (!Boolean.TRUE.equals(config.getRemoteBackupEnabled())) {
            return false;
        }

        if (config.getRemoteHost() == null || config.getRemoteHost().isBlank()) {
            log.warn("远程备份未配置主机地址");
            return false;
        }

        try {
            // 使用 ProcessBuilder 调用 scp 命令
            String remoteTarget = config.getRemoteUsername() + "@" + config.getRemoteHost() + ":"
                    + config.getRemotePath() + "/" + file.getName();

            ProcessBuilder pb = new ProcessBuilder("scp", "-P",
                    String.valueOf(config.getRemotePort() != null ? config.getRemotePort() : 22),
                    "-o", "StrictHostKeyChecking=no",
                    file.getAbsolutePath(), remoteTarget);

            // 如果配置了密码，使用 sshpass
            if (config.getRemotePassword() != null && !config.getRemotePassword().isBlank()) {
                pb = new ProcessBuilder("sshpass", "-p", config.getRemotePassword(),
                        "scp", "-P", String.valueOf(config.getRemotePort() != null ? config.getRemotePort() : 22),
                        "-o", "StrictHostKeyChecking=no",
                        file.getAbsolutePath(), remoteTarget);
            }

            pb.redirectErrorStream(true);
            Process process = pb.start();

            // 读取输出
            String output = new String(process.getInputStream().readAllBytes());
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                log.info("远程备份成功: {} -> {}:{}", file.getName(), config.getRemoteHost(), config.getRemotePath());
                return true;
            } else {
                log.error("远程备份失败: exitCode={}, output={}", exitCode, output);
                return false;
            }
        } catch (IOException | InterruptedException e) {
            log.error("远程备份异常: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 测试远程连接
     */
    public boolean testConnection(BackupConfig config) {
        if (config.getRemoteHost() == null || config.getRemoteHost().isBlank()) {
            return false;
        }

        try {
            ProcessBuilder pb;
            String sshTarget = config.getRemoteUsername() + "@" + config.getRemoteHost();

            if (config.getRemotePassword() != null && !config.getRemotePassword().isBlank()) {
                pb = new ProcessBuilder("sshpass", "-p", config.getRemotePassword(),
                        "ssh", "-p", String.valueOf(config.getRemotePort() != null ? config.getRemotePort() : 22),
                        "-o", "StrictHostKeyChecking=no",
                        "-o", "ConnectTimeout=5",
                        sshTarget, "echo ok");
            } else {
                pb = new ProcessBuilder("ssh", "-p",
                        String.valueOf(config.getRemotePort() != null ? config.getRemotePort() : 22),
                        "-o", "StrictHostKeyChecking=no",
                        "-o", "ConnectTimeout=5",
                        sshTarget, "echo ok");
            }

            pb.redirectErrorStream(true);
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (IOException | InterruptedException e) {
            log.error("远程连接测试失败: {}", e.getMessage());
            return false;
        }
    }
}
