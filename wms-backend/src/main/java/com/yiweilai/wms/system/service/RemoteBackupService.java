package com.yiweilai.wms.system.service;

import com.yiweilai.wms.system.entity.BackupConfig;
import lombok.extern.slf4j.Slf4j;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 远程备份服务 - 使用 sshj（纯 Java SSH）跨平台支持
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

        SSHClient ssh = new SSHClient();
        try {
            ssh.addHostKeyVerifier(new PromiscuousVerifier());
            ssh.setConnectTimeout(10000);

            int port = config.getRemotePort() != null ? config.getRemotePort() : 22;
            ssh.connect(config.getRemoteHost(), port);

            // 认证
            if (config.getRemotePassword() != null && !config.getRemotePassword().isBlank()) {
                ssh.authPassword(config.getRemoteUsername(), config.getRemotePassword());
            } else {
                ssh.authPublickey(config.getRemoteUsername());
            }

            // SFTP 上传
            try (SFTPClient sftp = ssh.newSFTPClient()) {
                String remotePath = config.getRemotePath() + "/" + file.getName();
                sftp.put(file.getAbsolutePath(), remotePath);
            }

            log.info("远程备份成功: {} -> {}:{}", file.getName(), config.getRemoteHost(), config.getRemotePath());
            return true;
        } catch (IOException e) {
            log.error("远程备份异常: {}", e.getMessage());
            return false;
        } finally {
            try {
                ssh.disconnect();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * 测试远程连接
     */
    public boolean testConnection(BackupConfig config) {
        if (config.getRemoteHost() == null || config.getRemoteHost().isBlank()) {
            return false;
        }

        SSHClient ssh = new SSHClient();
        try {
            ssh.addHostKeyVerifier(new PromiscuousVerifier());
            ssh.setConnectTimeout(5000);

            int port = config.getRemotePort() != null ? config.getRemotePort() : 22;
            ssh.connect(config.getRemoteHost(), port);

            // 认证
            if (config.getRemotePassword() != null && !config.getRemotePassword().isBlank()) {
                ssh.authPassword(config.getRemoteUsername(), config.getRemotePassword());
            } else {
                ssh.authPublickey(config.getRemoteUsername());
            }

            return true;
        } catch (IOException e) {
            log.error("远程连接测试失败: {}", e.getMessage());
            return false;
        } finally {
            try {
                ssh.disconnect();
            } catch (IOException ignored) {
            }
        }
    }
}
