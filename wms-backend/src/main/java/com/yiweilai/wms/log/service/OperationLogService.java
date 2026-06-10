package com.yiweilai.wms.log.service;

import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.log.dto.OperationLogQueryDTO;
import com.yiweilai.wms.log.entity.OperationLog;
import com.yiweilai.wms.log.vo.OperationLogVO;

/**
 * 操作日志 Service
 */
public interface OperationLogService {

    /**
     * 分页查询日志
     */
    PageResult<OperationLogVO> findByPage(OperationLogQueryDTO query);

    /**
     * 记录操作日志
     */
    void saveLog(OperationLog log);
}
