package com.yiweilai.wms.system.service.impl;

import com.yiweilai.wms.exception.BusinessException;
import com.yiweilai.wms.exception.ErrorCode;
import com.yiweilai.wms.system.entity.BackupRecord;
import com.yiweilai.wms.system.mapper.BackupRecordMapper;
import com.yiweilai.wms.system.service.BackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据库备份 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BackupServiceImpl implements BackupService {

    private final BackupRecordMapper backupRecordMapper;
    private final DataSource dataSource;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String datasourceUsername;

    @Value("${spring.datasource.password}")
    private String datasourcePassword;

    @Value("${backup.mysqldump-path:mysqldump}")
    private String mysqldumpPath;

    private static final String DEFAULT_BACKUP_DIR = "backups";
    private static final DateTimeFormatter FILE_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    // 需要增量备份的表（有 updated_at 字段的业务表）
    private static final String[] INCREMENTAL_TABLES = {
            "product", "product_sku", "product_barcode",
            "stock", "stock_log", "stock_alert_config",
            "warehouse", "warehouse_area", "warehouse_shelf",
            "sales_order", "sales_order_item",
            "outbound_order", "outbound_order_item",
            "return_order", "return_order_item",
            "exchange_order", "exchange_order_item",
            "express_company", "express_fee_template",
            "platform"
    };

    @Override
    public BackupRecord fullBackup(String backupPath) {
        String dir = (backupPath != null && !backupPath.isBlank()) ? backupPath : DEFAULT_BACKUP_DIR;
        String timestamp = LocalDateTime.now().format(FILE_DATE_FMT);
        String fileName = "wms_full_" + timestamp + ".sql";
        String filePath = dir + File.separator + fileName;

        // 确保目录存在
        File dirFile = new File(dir);
        if (!dirFile.exists()) {
            dirFile.mkdirs();
        }

        // 解析数据库连接信息
        String[] dbInfo = parseDbInfo();

        BackupRecord record = new BackupRecord();
        record.setBackupType("FULL");
        record.setFileName(fileName);
        record.setFilePath(filePath);

        try {
            // 执行 mysqldump
            ProcessBuilder pb = new ProcessBuilder(
                    findMysqldump(),
                    "-h", dbInfo[0],
                    "-P", dbInfo[1],
                    "-u", dbInfo[2],
                    "-p" + dbInfo[3],
                    "--single-transaction",
                    "--routines",
                    "--triggers",
                    dbInfo[4]
            );
            pb.redirectErrorStream(false);

            Process process = pb.start();

            // 读取输出写入文件
            File outputFile = new File(filePath);
            try (InputStream is = process.getInputStream();
                 OutputStream os = new FileOutputStream(outputFile)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    os.write(buffer, 0, len);
                }
            }

            // 读取错误输出
            String errorOutput;
            try (InputStream es = process.getErrorStream()) {
                errorOutput = new String(es.readAllBytes(), StandardCharsets.UTF_8);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                record.setStatus("FAILED");
                record.setRemark("mysqldump 退出码: " + exitCode + ", 错误: " + errorOutput);
                backupRecordMapper.insert(record);
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "全量备份失败: " + errorOutput);
            }

            record.setFileSize(outputFile.length());
            record.setStatus("SUCCESS");
            record.setRemark("全量备份完成");
            backupRecordMapper.insert(record);

            log.info("全量备份成功: {}, 大小: {} 字节", filePath, outputFile.length());
            return record;

        } catch (IOException e) {
            record.setStatus("FAILED");
            record.setRemark("执行 mysqldump 失败: " + e.getMessage());
            backupRecordMapper.insert(record);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "全量备份失败，请确认服务器已安装 mysqldump: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            record.setStatus("FAILED");
            record.setRemark("备份被中断");
            backupRecordMapper.insert(record);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "备份被中断");
        }
    }

    @Override
    public BackupRecord incrementalBackup(String backupPath) {
        String dir = (backupPath != null && !backupPath.isBlank()) ? backupPath : DEFAULT_BACKUP_DIR;
        String timestamp = LocalDateTime.now().format(FILE_DATE_FMT);
        String fileName = "wms_incr_" + timestamp + ".sql";
        String filePath = dir + File.separator + fileName;

        // 确保目录存在
        File dirFile = new File(dir);
        if (!dirFile.exists()) {
            dirFile.mkdirs();
        }

        // 查询上次备份时间
        LocalDateTime lastBackupTime = backupRecordMapper.findLastBackupTime(null);

        BackupRecord record = new BackupRecord();
        record.setBackupType("INCREMENTAL");
        record.setFileName(fileName);
        record.setFilePath(filePath);

        try {
            StringBuilder sqlContent = new StringBuilder();
            sqlContent.append("-- WMS 增量备份\n");
            sqlContent.append("-- 备份时间: ").append(LocalDateTime.now()).append("\n");
            if (lastBackupTime != null) {
                sqlContent.append("-- 自: ").append(lastBackupTime).append(" 以来的变更数据\n");
            } else {
                sqlContent.append("-- 首次增量备份，导出全部数据\n");
            }
            sqlContent.append("-- ============================================\n\n");
            sqlContent.append("USE `wms`;\n\n");

            int totalRows = 0;

            try (Connection conn = dataSource.getConnection()) {
                for (String table : INCREMENTAL_TABLES) {
                    // 检查表是否有 updated_at 字段
                    boolean hasUpdatedAt = false;
                    try (ResultSet metaRs = conn.getMetaData().getColumns(null, null, table, "updated_at")) {
                        hasUpdatedAt = metaRs.next();
                    }

                    String sql;
                    if (lastBackupTime != null && hasUpdatedAt) {
                        sql = "SELECT * FROM `" + table + "` WHERE updated_at > ?";
                    } else {
                        sql = "SELECT * FROM `" + table + "`";
                    }

                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        if (lastBackupTime != null && hasUpdatedAt) {
                            ps.setTimestamp(1, Timestamp.valueOf(lastBackupTime));
                        }

                        try (ResultSet rs = ps.executeQuery()) {
                            ResultSetMetaData meta = rs.getMetaData();
                            int columnCount = meta.getColumnCount();

                            List<String> rows = new ArrayList<>();
                            while (rs.next()) {
                                StringBuilder insertSql = new StringBuilder();
                                insertSql.append("INSERT INTO `").append(table).append("` (");
                                for (int i = 1; i <= columnCount; i++) {
                                    if (i > 1) insertSql.append(", ");
                                    insertSql.append("`").append(meta.getColumnName(i)).append("`");
                                }
                                insertSql.append(") VALUES (");
                                for (int i = 1; i <= columnCount; i++) {
                                    if (i > 1) insertSql.append(", ");
                                    Object val = rs.getObject(i);
                                    if (val == null) {
                                        insertSql.append("NULL");
                                    } else if (val instanceof String) {
                                        String escaped = ((String) val).replace("'", "\\'");
                                        insertSql.append("'").append(escaped).append("'");
                                    } else if (val instanceof Timestamp) {
                                        insertSql.append("'").append(val).append("'");
                                    } else if (val instanceof java.sql.Date) {
                                        insertSql.append("'").append(val).append("'");
                                    } else if (val instanceof Boolean) {
                                        insertSql.append(((Boolean) val) ? "1" : "0");
                                    } else {
                                        insertSql.append(val);
                                    }
                                }
                                insertSql.append(");\n");
                                rows.add(insertSql.toString());
                            }

                            if (!rows.isEmpty()) {
                                sqlContent.append("-- ").append(table).append(" (").append(rows.size()).append(" 行)\n");
                                sqlContent.append("INSERT IGNORE INTO `").append(table).append("` VALUES\n");
                                for (int i = 0; i < rows.size(); i++) {
                                    sqlContent.append(rows.get(i));
                                }
                                sqlContent.append("\n");
                                totalRows += rows.size();
                            }
                        }
                    }
                }
            }

            // 写入文件
            File outputFile = new File(filePath);
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8)) {
                writer.write(sqlContent.toString());
            }

            record.setFileSize(outputFile.length());
            record.setStatus("SUCCESS");
            record.setRemark("增量备份完成，共 " + totalRows + " 行变更数据");
            backupRecordMapper.insert(record);

            log.info("增量备份成功: {}, 行数: {}, 大小: {} 字节", filePath, totalRows, outputFile.length());
            return record;

        } catch (SQLException e) {
            log.error("增量备份失败", e);
            record.setStatus("FAILED");
            record.setRemark("数据库查询失败: " + e.getMessage());
            backupRecordMapper.insert(record);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "增量备份失败: " + e.getMessage());
        } catch (IOException e) {
            log.error("增量备份写文件失败", e);
            record.setStatus("FAILED");
            record.setRemark("写文件失败: " + e.getMessage());
            backupRecordMapper.insert(record);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "增量备份写文件失败: " + e.getMessage());
        }
    }

    @Override
    public List<BackupRecord> getBackupList() {
        return backupRecordMapper.findAll();
    }

    @Override
    public File getBackupFile(String fileName) {
        // 从记录中查找文件路径
        List<BackupRecord> records = backupRecordMapper.findAll();
        for (BackupRecord record : records) {
            if (record.getFileName().equals(fileName)) {
                File file = new File(record.getFilePath());
                if (file.exists()) {
                    return file;
                }
            }
        }
        // 尝试默认路径
        File defaultFile = new File(DEFAULT_BACKUP_DIR + File.separator + fileName);
        if (defaultFile.exists()) {
            return defaultFile;
        }
        throw new BusinessException(ErrorCode.NOT_FOUND, "备份文件不存在");
    }

    @Override
    public void deleteBackup(Long id) {
        BackupRecord record = backupRecordMapper.findById(id);
        if (record == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "备份记录不存在");
        }
        // 删除文件
        File file = new File(record.getFilePath());
        if (file.exists()) {
            file.delete();
        }
        backupRecordMapper.deleteById(id);
    }

    /**
     * 解析数据库连接信息：[host, port, username, password, database]
     */
    private String[] parseDbInfo() {
        // jdbc:mysql://192.168.199.136:3306/wms?...
        try {
            String url = datasourceUrl.replace("jdbc:mysql://", "");
            String hostPort = url.split("/")[0];
            String host = hostPort.split(":")[0];
            String port = hostPort.split(":")[1];
            String database = url.split("/")[1].split("\\?")[0];
            return new String[]{host, port, datasourceUsername, datasourcePassword, database};
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "解析数据库连接信息失败");
        }
    }

    /**
     * 查找 mysqldump 可执行文件路径
     * 优先使用配置的路径，其次自动检测常见安装位置
     */
    private String findMysqldump() {
        // 1. 如果配置了完整路径，直接使用
        if (mysqldumpPath != null && !mysqldumpPath.equals("mysqldump")) {
            File file = new File(mysqldumpPath);
            if (file.exists()) {
                return mysqldumpPath;
            }
        }

        // 2. 尝试直接调用（已在 PATH 中）
        try {
            Process test = new ProcessBuilder("mysqldump", "--version").start();
            int code = test.waitFor();
            if (code == 0) return "mysqldump";
        } catch (Exception ignored) {}

        // 3. 自动检测常见安装路径（Windows）
        String[] candidates = {
                "C:\\Program Files\\MySQL\\MySQL Server 8.4\\bin\\mysqldump.exe",
                "C:\\Program Files\\MySQL\\MySQL Server 8.0\\bin\\mysqldump.exe",
                "C:\\Program Files\\MySQL\\MySQL Server 5.7\\bin\\mysqldump.exe",
                "C:\\Program Files\\MySQL\\MySQL Server 5.5\\bin\\mysqldump.exe",
                "/usr/bin/mysqldump",
                "/usr/local/bin/mysqldump",
        };
        for (String path : candidates) {
            if (new File(path).exists()) {
                return path;
            }
        }

        throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                "找不到 mysqldump，请在 application.properties 中配置 backup.mysqldump-path");
    }
}
