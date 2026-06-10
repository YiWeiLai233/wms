package com.yiweilai.wms.log.mapper;

import com.yiweilai.wms.log.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 操作日志 Mapper
 */
@Mapper
public interface OperationLogMapper {

    /**
     * 分页查询日志
     */
    List<OperationLog> findByPage(@Param("userId") Long userId,
                                  @Param("operation") String operation,
                                  @Param("status") Integer status);

    /**
     * 新增日志
     */
    int insert(OperationLog log);
}
