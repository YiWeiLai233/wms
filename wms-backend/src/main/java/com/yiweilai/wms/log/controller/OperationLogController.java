package com.yiweilai.wms.log.controller;

import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.common.Result;
import com.yiweilai.wms.log.dto.OperationLogQueryDTO;
import com.yiweilai.wms.log.service.OperationLogService;
import com.yiweilai.wms.log.vo.OperationLogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 操作日志 Controller
 */
@RestController
@RequestMapping("/api/operation-logs")
@RequiredArgsConstructor
public class OperationLogController {

    private final OperationLogService operationLogService;

    /**
     * 操作日志列表（分页）
     */
    @GetMapping
    public Result<PageResult<OperationLogVO>> list(OperationLogQueryDTO query) {
        return Result.success(operationLogService.findByPage(query));
    }
}
