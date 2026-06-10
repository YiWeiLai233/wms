package com.yiweilai.wms.file.mapper;

import com.yiweilai.wms.file.entity.FileRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 文件记录 Mapper
 */
@Mapper
public interface FileRecordMapper {

    /**
     * 根据ID查询
     */
    FileRecord findById(@Param("id") Long id);

    /**
     * 新增文件记录
     */
    int insert(FileRecord fileRecord);

    /**
     * 删除文件记录
     */
    int deleteById(@Param("id") Long id);
}
