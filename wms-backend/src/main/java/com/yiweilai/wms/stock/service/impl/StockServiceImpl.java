package com.yiweilai.wms.stock.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.exception.BusinessException;
import com.yiweilai.wms.exception.ErrorCode;
import com.yiweilai.wms.stock.dto.StockAdjustDTO;
import com.yiweilai.wms.stock.dto.StockQueryDTO;
import com.yiweilai.wms.stock.entity.Stock;
import com.yiweilai.wms.stock.entity.StockLog;
import com.yiweilai.wms.stock.mapper.StockLogMapper;
import com.yiweilai.wms.stock.mapper.StockMapper;
import com.yiweilai.wms.stock.service.StockService;
import com.yiweilai.wms.stock.vo.StockVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 库存 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockServiceImpl implements StockService {

    private final StockMapper stockMapper;
    private final StockLogMapper stockLogMapper;

    @Override
    public PageResult<StockVO> findByPage(StockQueryDTO query) {
        PageHelper.startPage(query.getPage(), query.getSize());
        List<Stock> stocks = stockMapper.findByPage(
                query.getSkuId(),
                query.getSkuCode(),
                query.getSkuName(),
                query.getProductName(),
                query.getWarehouseId(),
                query.getStockType());

        PageInfo<Stock> pageInfo = new PageInfo<>(stocks);

        List<StockVO> voList = stocks.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        PageResult<StockVO> result = new PageResult<>();
        result.setTotal(pageInfo.getTotal());
        result.setList(voList);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjust(StockAdjustDTO dto) {
        // 查询或创建库存记录
        Stock stock = stockMapper.findBySkuAndWarehouse(dto.getSkuId(), dto.getWarehouseId());

        if (stock == null) {
            // 新增库存记录
            stock = new Stock();
            stock.setSkuId(dto.getSkuId());
            stock.setWarehouseId(dto.getWarehouseId());
            stock.setQuantity(dto.getQuantity());
            stock.setLockedQty(0);
            stock.setDefectiveQty(0);
            stockMapper.insert(stock);

            // 写流水
            writeLog("ADJUST", "ADJUST_" + stock.getId(), dto.getSkuId(),
                    dto.getWarehouseId(),
                    0, dto.getQuantity(), dto.getQuantity(), dto.getRemark());
        } else {
            // 调整库存
            int beforeQty = stock.getQuantity();
            int afterQty = beforeQty + dto.getQuantity();

            if (afterQty < 0) {
                throw new BusinessException(ErrorCode.STOCK_NOT_ENOUGH);
            }

            stockMapper.updateQuantity(stock.getId(), afterQty);

            // 写流水
            writeLog("ADJUST", "ADJUST_" + stock.getId(), dto.getSkuId(),
                    dto.getWarehouseId(),
                    beforeQty, dto.getQuantity(), afterQty, dto.getRemark());
        }
    }

    private void writeLog(String bizType, String bizNo, Long skuId, Long warehouseId,
                          int before, int change, int after, String remark) {
        StockLog log = new StockLog();
        log.setBizType(bizType);
        log.setBizNo(bizNo);
        log.setSkuId(skuId);
        log.setWarehouseId(warehouseId);
        log.setQuantityBefore(before);
        log.setQuantityChange(change);
        log.setQuantityAfter(after);
        log.setRemark(remark);
        stockLogMapper.insert(log);
    }

    private StockVO convertToVO(Stock stock) {
        StockVO vo = new StockVO();
        BeanUtils.copyProperties(stock, vo);

        // 计算总数量
        int total = 0;
        if (stock.getQuantity() != null) total += stock.getQuantity();
        if (stock.getLockedQty() != null) total += stock.getLockedQty();
        if (stock.getDefectiveQty() != null) total += stock.getDefectiveQty();
        vo.setTotalQuantity(total);

        return vo;
    }
}
