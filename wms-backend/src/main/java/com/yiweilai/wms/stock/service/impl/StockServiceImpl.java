package com.yiweilai.wms.stock.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.exception.BusinessException;
import com.yiweilai.wms.exception.ErrorCode;
import com.yiweilai.wms.alert.entity.StockAlertConfig;
import com.yiweilai.wms.alert.service.StockAlertConfigService;
import com.yiweilai.wms.stock.dto.BatchStockAdjustDTO;
import com.yiweilai.wms.stock.dto.StockAdjustDTO;
import com.yiweilai.wms.stock.dto.StockQueryDTO;
import com.yiweilai.wms.stock.entity.Stock;
import com.yiweilai.wms.stock.entity.StockLog;
import com.yiweilai.wms.stock.mapper.StockLogMapper;
import com.yiweilai.wms.stock.mapper.StockMapper;
import com.yiweilai.wms.stock.service.StockService;
import com.yiweilai.wms.stock.vo.StockVO;
import com.yiweilai.wms.warehouse.entity.Warehouse;
import com.yiweilai.wms.warehouse.mapper.WarehouseMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Map;
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
    private final WarehouseMapper warehouseMapper;
    private final StockAlertConfigService stockAlertConfigService;

    @Override
    public PageResult<StockVO> findByPage(StockQueryDTO query) {
        PageHelper.startPage(query.getPage(), query.getSize());
        List<Stock> stocks = stockMapper.findByPage(
                query.getSkuId(),
                query.getSkuCode(),
                query.getSkuName(),
                query.getProductName(),
                query.getWarehouseId(),
                query.getStockType(),
                null);

        PageInfo<Stock> pageInfo = new PageInfo<>(stocks);

        // 批量查询预警配置，避免N+1
        List<Long> skuIds = stocks.stream().map(Stock::getSkuId).distinct().collect(Collectors.toList());
        Map<Long, StockAlertConfig> configMap = stockAlertConfigService.findConfigMapBySkuIds(skuIds);

        List<StockVO> voList = stocks.stream()
                .map(stock -> convertToVO(stock, configMap.get(stock.getSkuId())))
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
//            stock.setDefectiveQty(0);
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchAdjust(BatchStockAdjustDTO dto) {
        for (BatchStockAdjustDTO.StockAdjustItem item : dto.getItems()) {
            StockAdjustDTO adjustDTO = new StockAdjustDTO();
            adjustDTO.setSkuId(item.getSkuId());
            adjustDTO.setWarehouseId(dto.getWarehouseId());
            adjustDTO.setQuantity(item.getQuantity());
            adjustDTO.setRemark(dto.getRemark());
            adjust(adjustDTO);
        }
    }

    @Override
    public List<StockVO> findByWarehouseType(String warehouseType, String skuCode, String skuName) {
        List<Stock> stocks = stockMapper.findByWarehouseType(warehouseType, skuCode, skuName);
        List<Long> skuIds = stocks.stream().map(Stock::getSkuId).distinct().collect(Collectors.toList());
        Map<Long, StockAlertConfig> configMap = stockAlertConfigService.findConfigMapBySkuIds(skuIds);
        return stocks.stream()
                .map(stock -> convertToVO(stock, configMap.get(stock.getSkuId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmSellable(Long stockId, Long targetWarehouseId, Integer quantity) {
        Stock stock = stockMapper.findById(stockId);
        if (stock == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "库存记录不存在");
        }
        if (quantity <= 0 || quantity > stock.getQuantity()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "转移数量无效");
        }

        // 从次品仓扣减
        stockMapper.deductQuantity(stockId, quantity);

        // 增加到目标普通仓
        Stock targetStock = stockMapper.findBySkuAndWarehouse(stock.getSkuId(), targetWarehouseId);
        int beforeTargetQty = 0;
        if (targetStock == null) {
            targetStock = new Stock();
            targetStock.setSkuId(stock.getSkuId());
            targetStock.setWarehouseId(targetWarehouseId);
            targetStock.setQuantity(quantity);
            targetStock.setLockedQty(0);
            stockMapper.insert(targetStock);
        } else {
            beforeTargetQty = targetStock.getQuantity();
            stockMapper.addQuantity(targetStock.getId(), quantity);
        }

        // 写流水
        writeLog("TRANSFER", "SELLABLE_" + stockId, stock.getSkuId(), stock.getWarehouseId(),
                stock.getQuantity() + quantity, -quantity, stock.getQuantity(), "确认可售，转出次品仓");
        writeLog("TRANSFER", "SELLABLE_" + stockId, stock.getSkuId(), targetWarehouseId,
                beforeTargetQty, quantity, beforeTargetQty + quantity, "可售商品入库");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmDispose(Long stockId, Integer quantity) {
        Stock stock = stockMapper.findById(stockId);
        if (stock == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "库存记录不存在");
        }
        if (quantity <= 0 || quantity > stock.getQuantity()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "报废数量无效");
        }

        // 从报废仓扣减
        stockMapper.deductQuantity(stockId, quantity);

        // 写流水
        writeLog("DISPOSE", "DISPOSE_" + stockId, stock.getSkuId(), stock.getWarehouseId(),
                stock.getQuantity() + quantity, -quantity, stock.getQuantity(), "确认报废处置");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmScrap(Long stockId, Integer quantity) {
        Stock stock = stockMapper.findById(stockId);
        if (stock == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "库存记录不存在");
        }
        if (quantity <= 0 || quantity > stock.getQuantity()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "转移数量无效");
        }

        // 查找报废仓
        Warehouse scrapWarehouse = warehouseMapper.findByType("SCRAP");
        if (scrapWarehouse == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "未配置报废仓");
        }

        // 从次品仓扣减
        stockMapper.deductQuantity(stockId, quantity);

        // 增加到报废仓
        Stock targetStock = stockMapper.findBySkuAndWarehouse(stock.getSkuId(), scrapWarehouse.getId());
        int beforeTargetQty = 0;
        if (targetStock == null) {
            targetStock = new Stock();
            targetStock.setSkuId(stock.getSkuId());
            targetStock.setWarehouseId(scrapWarehouse.getId());
            targetStock.setQuantity(quantity);
            targetStock.setLockedQty(0);
            stockMapper.insert(targetStock);
        } else {
            beforeTargetQty = targetStock.getQuantity();
            stockMapper.addQuantity(targetStock.getId(), quantity);
        }

        // 写流水
        writeLog("TRANSFER", "SCRAP_" + stockId, stock.getSkuId(), stock.getWarehouseId(),
                stock.getQuantity() + quantity, -quantity, stock.getQuantity(), "次品转报废仓");
        writeLog("TRANSFER", "SCRAP_" + stockId, stock.getSkuId(), scrapWarehouse.getId(),
                beforeTargetQty, quantity, beforeTargetQty + quantity, "报废入库");
    }

    private void writeLog(String bizType, String bizNo, Long skuId, Long warehouseId,
                          int before, int change, int after, String remark) {
        StockLog stockLog = new StockLog();
        stockLog.setBizType(bizType);
        stockLog.setBizNo(bizNo);
        stockLog.setSkuId(skuId);
        stockLog.setWarehouseId(warehouseId);
        stockLog.setQuantityBefore(before);
        stockLog.setQuantityChange(change);
        stockLog.setQuantityAfter(after);
        stockLog.setRemark(remark);

        // 从请求上下文获取当前操作人
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                Object userId = attrs.getRequest().getAttribute("userId");
                Object username = attrs.getRequest().getAttribute("username");
                if (userId instanceof Long) {
                    stockLog.setOperatorId((Long) userId);
                }
                if (username instanceof String) {
                    stockLog.setOperatorName((String) username);
                }
            }
        } catch (Exception e) {
            log.debug("获取操作人信息失败", e);
        }

        stockLogMapper.insert(stockLog);
    }

    // 默认预警阈值常量
    private static final int DEFAULT_LOW_STOCK_THRESHOLD = 10;
    private static final int DEFAULT_OUT_OF_STOCK_THRESHOLD = 0;

    private StockVO convertToVO(Stock stock, StockAlertConfig config) {
        StockVO vo = new StockVO();
        BeanUtils.copyProperties(stock, vo);

        // 计算总数量
        int total = 0;
        if (stock.getQuantity() != null) total += stock.getQuantity();
        if (stock.getLockedQty() != null) total += stock.getLockedQty();
        vo.setTotalQuantity(total);

        // 计算预警状态
        int lowThreshold = DEFAULT_LOW_STOCK_THRESHOLD;
        int outThreshold = DEFAULT_OUT_OF_STOCK_THRESHOLD;
        if (config != null) {
            lowThreshold = config.getLowStockThreshold();
            outThreshold = config.getOutOfStockThreshold();
        }
        vo.setLowStockThreshold(lowThreshold);
        vo.setOutOfStockThreshold(outThreshold);

        int qty = stock.getQuantity() != null ? stock.getQuantity() : 0;
        String status;
        if (qty <= outThreshold) {
            status = "OUT_OF_STOCK";
        } else if (qty <= lowThreshold) {
            status = "LOW_STOCK";
        } else {
            status = "NORMAL";
        }
        vo.setStockAlertStatus(status);
        vo.setStockAlertStatusName(getStatusName(status));

        return vo;
    }

    private String getStatusName(String status) {
        switch (status) {
            case "OUT_OF_STOCK": return "缺货";
            case "LOW_STOCK": return "低库存";
            default: return "库存正常";
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductStock(Long skuId, int quantity, String bizNo, Long warehouseId, String bizType, String remark) {
        int remaining = quantity;
        while (remaining > 0) {
            List<Stock> availableStocks = stockMapper.findAvailableBySkuAndWarehouse(skuId, warehouseId);
            if (availableStocks.isEmpty()) {
                throw new BusinessException(ErrorCode.STOCK_NOT_ENOUGH, "SKU[" + skuId + "]库存不足，剩余需扣: " + remaining);
            }

            boolean deducted = false;
            for (Stock stock : availableStocks) {
                int beforeQty = stock.getQuantity() == null ? 0 : stock.getQuantity();
                int deductQty = Math.min(beforeQty, remaining);
                if (deductQty <= 0) continue;

                int affected = stockMapper.deductQuantity(stock.getId(), deductQty);
                if (affected == 0) continue;

                // 写流水
                writeLog(bizType, bizNo, skuId, warehouseId,
                        beforeQty, -deductQty, beforeQty - deductQty, remark);

                remaining -= deductQty;
                deducted = true;
                break; // 扣成功了就跳出 for 循环，重新查询最新库存
            }

            if (!deducted) {
                throw new BusinessException(ErrorCode.STOCK_NOT_ENOUGH, "SKU[" + skuId + "]库存不足，剩余需扣: " + remaining);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addStock(Long skuId, int quantity, String bizNo, Long warehouseId, String bizType, String remark) {
        Stock stock = stockMapper.findBySkuAndWarehouse(skuId, warehouseId);
        int beforeQty;

        if (stock == null) {
            // 新增库存记录
            stock = new Stock();
            stock.setSkuId(skuId);
            stock.setWarehouseId(warehouseId);
            stock.setQuantity(quantity);
            stock.setLockedQty(0);
            stockMapper.insert(stock);
            beforeQty = 0;
        } else {
            beforeQty = stock.getQuantity() == null ? 0 : stock.getQuantity();
            stockMapper.addQuantity(stock.getId(), quantity);
        }

        // 写流水
        writeLog(bizType, bizNo, skuId, warehouseId,
                beforeQty, quantity, beforeQty + quantity, remark);
    }
}
