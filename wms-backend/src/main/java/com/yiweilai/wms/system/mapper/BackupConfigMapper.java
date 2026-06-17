package com.yiweilai.wms.system.mapper;

import com.yiweilai.wms.system.entity.BackupConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 备份配置 Mapper
 */
@Mapper
public interface BackupConfigMapper {

    /**
     * 获取配置（只有一条记录）
     */
    BackupConfig getConfig();

    /**
     * 新增配置
     */
    int insert(BackupConfig config);

    /**
     * 更新配置
     */
    int update(BackupConfig config);
}
