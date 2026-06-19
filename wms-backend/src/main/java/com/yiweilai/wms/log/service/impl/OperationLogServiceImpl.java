package com.yiweilai.wms.log.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.log.dto.OperationLogQueryDTO;
import com.yiweilai.wms.log.entity.OperationLog;
import com.yiweilai.wms.log.mapper.OperationLogMapper;
import com.yiweilai.wms.log.service.OperationLogService;
import com.yiweilai.wms.log.vo.OperationLogVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 操作日志 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements OperationLogService {

    private final OperationLogMapper operationLogMapper;

    @Override
    public PageResult<OperationLogVO> findByPage(OperationLogQueryDTO query) {
        PageHelper.startPage(query.getPage(), query.getSize());
        List<OperationLog> logs = operationLogMapper.findByPage(
                query.getUserId(), query.getModule(), query.getAction());

        PageInfo<OperationLog> pageInfo = new PageInfo<>(logs);

        List<OperationLogVO> voList = logs.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        PageResult<OperationLogVO> result = new PageResult<>();
        result.setTotal(pageInfo.getTotal());
        result.setList(voList);
        return result;
    }

    @Override
    @Async
    public void saveLog(OperationLog operationLog) {
        try {
            operationLogMapper.insert(operationLog);
        } catch (Exception e) {
            log.error("保存操作日志失败", e);
        }
    }

    private OperationLogVO convertToVO(OperationLog log) {
        OperationLogVO vo = new OperationLogVO();
        BeanUtils.copyProperties(log, vo);
        return vo;
    }
}
