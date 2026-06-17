package com.yiweilai.wms.system.mapper;

import com.yiweilai.wms.system.entity.BackupRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 备份记录 Mapper
 */
@Mapper
public interface BackupRecordMapper {

    /**
     * 查询所有备份记录
     */
    List<BackupRecord> findAll();

    /**
     * 根据ID查询
     */
    BackupRecord findById(@Param("id") Long id);

    /**
     * 查询最近一次备份时间
     */
    LocalDateTime findLastBackupTime(@Param("backupType") String backupType);

    /**
     * 新增
     */
    int insert(BackupRecord record);

    /**
     * 删除
     */
    int deleteById(@Param("id") Long id);
}
