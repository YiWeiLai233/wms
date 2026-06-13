package com.yiweilai.wms.stock.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.stock.dto.StockLogQueryDTO;
import com.yiweilai.wms.stock.entity.StockLog;
import com.yiweilai.wms.stock.mapper.StockLogMapper;
import com.yiweilai.wms.stock.service.StockLogService;
import com.yiweilai.wms.stock.vo.StockLogVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 库存流水 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockLogServiceImpl implements StockLogService {

    private final StockLogMapper stockLogMapper;

    @Override
    public PageResult<StockLogVO> findByPage(StockLogQueryDTO query) {
        PageHelper.startPage(query.getPage(), query.getSize());
        List<StockLog> logs = stockLogMapper.findByPage(
                query.getBizType(),
                query.getBizNo(),
                query.getPlatformOrderNo(),
                query.getSkuId(),
                query.getWarehouseId(),
                query.getStartTime(),
                query.getEndTime());

        PageInfo<StockLog> pageInfo = new PageInfo<>(logs);

        List<StockLogVO> voList = logs.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        PageResult<StockLogVO> result = new PageResult<>();
        result.setTotal(pageInfo.getTotal());
        result.setList(voList);
        return result;
    }

    private StockLogVO convertToVO(StockLog log) {
        StockLogVO vo = new StockLogVO();
        BeanUtils.copyProperties(log, vo);
        return vo;
    }
}
