package com.yiweilai.wms.alert.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.yiweilai.wms.alert.dto.StockAlertConfigCreateDTO;
import com.yiweilai.wms.alert.dto.StockAlertConfigQueryDTO;
import com.yiweilai.wms.alert.dto.StockAlertConfigUpdateDTO;
import com.yiweilai.wms.alert.entity.StockAlertConfig;
import com.yiweilai.wms.alert.mapper.StockAlertConfigMapper;
import com.yiweilai.wms.alert.service.StockAlertConfigService;
import com.yiweilai.wms.alert.vo.StockAlertConfigVO;
import com.yiweilai.wms.alert.vo.StockAlertStatisticsVO;
import com.yiweilai.wms.alert.vo.StockAlertStatusVO;
import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.exception.BusinessException;
import com.yiweilai.wms.exception.ErrorCode;
import com.yiweilai.wms.stock.entity.Stock;
import com.yiweilai.wms.stock.mapper.StockMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 库存预警配置 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockAlertConfigServiceImpl implements StockAlertConfigService {

    private final StockAlertConfigMapper stockAlertConfigMapper;
    private final StockMapper stockMapper;

    /** 默认低库存阈值 */
    private static final int DEFAULT_LOW_STOCK_THRESHOLD = 10;

    /** 默认缺货阈值 */
    private static final int DEFAULT_OUT_OF_STOCK_THRESHOLD = 0;

    @Override
    public PageResult<StockAlertConfigVO> findByPage(StockAlertConfigQueryDTO query) {
        PageHelper.startPage(query.getPage(), query.getSize());
        List<StockAlertConfigVO> list = stockAlertConfigMapper.findByPage(
                query.getSkuCode(),
                query.getSkuName(),
                query.getWarehouseId(),
                query.getEnabled());

        PageInfo<StockAlertConfigVO> pageInfo = new PageInfo<>(list);

        PageResult<StockAlertConfigVO> result = new PageResult<>();
        result.setTotal(pageInfo.getTotal());
        result.setList(
                list);
        return result;
    }

    @Override
    public Long create(StockAlertConfigCreateDTO dto) {
        // 校验：缺货阈值不能大于低库存阈值
        if (dto.getOutOfStockThreshold() > dto.getLowStockThreshold()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "缺货阈值不能大于低库存阈值");
        }

        // 检查是否已存在相同SKU+仓库的配置
        StockAlertConfig existing = stockAlertConfigMapper.findBySkuAndWarehouse(dto.getSkuId(), dto.getWarehouseId());
        if (existing != null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该SKU已存在预警配置");
        }

        StockAlertConfig config = new StockAlertConfig();
        config.setSkuId(dto.getSkuId());
        config.setWarehouseId(dto.getWarehouseId());
        config.setLowStockThreshold(dto.getLowStockThreshold());
        config.setOutOfStockThreshold(dto.getOutOfStockThreshold());
        config.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : 1);
        config.setRemark(dto.getRemark());
        config.setDeleted(0);

        stockAlertConfigMapper.insert(config);
        return config.getId();
    }

    @Override
    public void update(Long id, StockAlertConfigUpdateDTO dto) {
        StockAlertConfig config = stockAlertConfigMapper.findById(id);
        if (config == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "预警配置不存在");
        }

        // 校验：缺货阈值不能大于低库存阈值
        if (dto.getOutOfStockThreshold() > dto.getLowStockThreshold()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "缺货阈值不能大于低库存阈值");
        }

        config.setLowStockThreshold(dto.getLowStockThreshold());
        config.setOutOfStockThreshold(dto.getOutOfStockThreshold());
        if (dto.getEnabled() != null) {
            config.setEnabled(dto.getEnabled());
        }
        if (dto.getRemark() != null) {
            config.setRemark(dto.getRemark());
        }

        stockAlertConfigMapper.update(config);
    }

    @Override
    public void delete(Long id) {
        StockAlertConfig config = stockAlertConfigMapper.findById(id);
        if (config == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "预警配置不存在");
        }
        stockAlertConfigMapper.deleteById(id);
    }

    @Override
    public void updateEnabled(Long id, Integer enabled) {
        StockAlertConfig config = stockAlertConfigMapper.findById(id);
        if (config == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "预警配置不存在");
        }
        stockAlertConfigMapper.updateEnabled(id, enabled);
    }

    @Override
    public List<StockAlertStatusVO> getLowStockList() {
        // 只查询普通仓库存
        List<Stock> allStocks = stockMapper.findByPage(null, null, null, null, null, null, "NORMAL");
        return allStocks.stream()
                .map(stock -> {
                    String status = calculateStockStatus(stock.getSkuId(), stock.getWarehouseId(), stock.getQuantity());
                    if ("LOW_STOCK".equals(status)) {
                        return buildAlertStatusVO(stock, status);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<StockAlertStatusVO> getOutOfStockList() {
        // 只查询普通仓库存
        List<Stock> allStocks = stockMapper.findByPage(null, null, null, null, null, null, "NORMAL");
        return allStocks.stream()
                .map(stock -> {
                    String status = calculateStockStatus(stock.getSkuId(), stock.getWarehouseId(), stock.getQuantity());
                    if ("OUT_OF_STOCK".equals(status)) {
                        return buildAlertStatusVO(stock, status);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public StockAlertStatisticsVO getStatistics() {
        // 只查询普通仓库存
        List<Stock> allStocks = stockMapper.findByPage(null, null, null, null, null, null, "NORMAL");

        long normalCount = 0;
        long lowStockCount = 0;
        long outOfStockCount = 0;

        for (Stock stock : allStocks) {
            String status = calculateStockStatus(stock.getSkuId(), stock.getWarehouseId(), stock.getQuantity());
            switch (status) {
                case "OUT_OF_STOCK":
                    outOfStockCount++;
                    break;
                case "LOW_STOCK":
                    lowStockCount++;
                    break;
                default:
                    normalCount++;
                    break;
            }
        }

        StockAlertStatisticsVO vo = new StockAlertStatisticsVO();
        vo.setNormalCount(normalCount);
        vo.setLowStockCount(lowStockCount);
        vo.setOutOfStockCount(outOfStockCount);
        return vo;
    }

    @Override
    public String calculateStockStatus(Long skuId, Long warehouseId, Integer quantity) {
        // 1. 查询启用的预警配置（优先仓库专属，其次通用）
        StockAlertConfig config = stockAlertConfigMapper.findEnabledConfig(skuId, warehouseId);

        int lowThreshold = DEFAULT_LOW_STOCK_THRESHOLD;
        int outThreshold = DEFAULT_OUT_OF_STOCK_THRESHOLD;

        if (config != null) {
            lowThreshold = config.getLowStockThreshold();
            outThreshold = config.getOutOfStockThreshold();
        }

        // 2. 判断状态
        if (quantity <= outThreshold) {
            return "OUT_OF_STOCK";
        }
        if (quantity <= lowThreshold) {
            return "LOW_STOCK";
        }
        return "NORMAL";
    }

    @Override
    public Map<Long, StockAlertConfig> findConfigMapBySkuIds(List<Long> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<StockAlertConfig> configs = stockAlertConfigMapper.findEnabledBySkuIds(skuIds);
        // 每个SKU只保留一条配置（优先仓库专属，已按排序排好，取第一条）
        Map<Long, StockAlertConfig> map = new HashMap<>();
        for (StockAlertConfig config : configs) {
            map.putIfAbsent(config.getSkuId(), config);
        }
        return map;
    }

    private StockAlertStatusVO buildAlertStatusVO(Stock stock, String status) {
        StockAlertConfig config = stockAlertConfigMapper.findEnabledConfig(stock.getSkuId(), stock.getWarehouseId());

        int lowThreshold = DEFAULT_LOW_STOCK_THRESHOLD;
        int outThreshold = DEFAULT_OUT_OF_STOCK_THRESHOLD;
        if (config != null) {
            lowThreshold = config.getLowStockThreshold();
            outThreshold = config.getOutOfStockThreshold();
        }

        StockAlertStatusVO vo = new StockAlertStatusVO();
        vo.setStockId(stock.getId());
        vo.setSkuId(stock.getSkuId());
        vo.setSkuCode(stock.getSkuCode());
        vo.setSkuName(stock.getSkuName());
        vo.setWarehouseId(stock.getWarehouseId());
        vo.setWarehouseName(stock.getWarehouseName());
        vo.setQuantity(stock.getQuantity());
        vo.setLockedQty(stock.getLockedQty());
        vo.setLowStockThreshold(lowThreshold);
        vo.setOutOfStockThreshold(outThreshold);
        vo.setAlertStatus(status);
        vo.setAlertStatusName(getStatusName(status));
        vo.setUpdatedAt(stock.getUpdatedAt());
        return vo;
    }

    private String getStatusName(String status) {
        switch (status) {
            case "OUT_OF_STOCK":
                return "缺货";
            case "LOW_STOCK":
                return "低库存";
            default:
                return "库存正常";
        }
    }
}
