package com.yiweilai.wms.warehouse.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.config.CacheService;
import com.yiweilai.wms.exception.BusinessException;
import com.yiweilai.wms.exception.ErrorCode;
import com.yiweilai.wms.warehouse.dto.WarehouseQueryDTO;
import com.yiweilai.wms.warehouse.dto.WarehouseSaveDTO;
import com.yiweilai.wms.warehouse.entity.Warehouse;
import com.yiweilai.wms.warehouse.mapper.WarehouseMapper;
import com.yiweilai.wms.warehouse.service.WarehouseService;
import com.yiweilai.wms.warehouse.vo.WarehouseVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 仓库 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseMapper warehouseMapper;
    private final CacheService cacheService;

    @Override
    public PageResult<WarehouseVO> findByPage(WarehouseQueryDTO query) {
        PageHelper.startPage(query.getPage(), query.getSize());
        List<Warehouse> warehouses = warehouseMapper.findByPage(query.getKeyword(), query.getStatus());

        PageInfo<Warehouse> pageInfo = new PageInfo<>(warehouses);

        List<WarehouseVO> voList = warehouses.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        PageResult<WarehouseVO> result = new PageResult<>();
        result.setTotal(pageInfo.getTotal());
        result.setList(voList);
        return result;
    }

    @Override
    public WarehouseVO getById(Long id) {
        String cacheKey = "cache:warehouse:" + id;
        WarehouseVO cached = cacheService.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        Warehouse warehouse = warehouseMapper.findById(id);
        if (warehouse == null) {
            throw new BusinessException(ErrorCode.WAREHOUSE_NOT_FOUND);
        }
        WarehouseVO vo = convertToVO(warehouse);
        cacheService.set(cacheKey, vo, 30, TimeUnit.MINUTES);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(WarehouseSaveDTO dto) {
        // 检查编码唯一性
        Warehouse existing = warehouseMapper.findByCode(dto.getCode());
        if (existing != null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仓库编码已存在");
        }

        Warehouse warehouse = new Warehouse();
        BeanUtils.copyProperties(dto, warehouse);
        warehouseMapper.insert(warehouse);
        return warehouse.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(WarehouseSaveDTO dto) {
        Warehouse warehouse = warehouseMapper.findById(dto.getId());
        if (warehouse == null) {
            throw new BusinessException(ErrorCode.WAREHOUSE_NOT_FOUND);
        }

        // 检查编码唯一性（排除自身）
        if (!warehouse.getCode().equals(dto.getCode())) {
            Warehouse existing = warehouseMapper.findByCode(dto.getCode());
            if (existing != null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "仓库编码已存在");
            }
        }

        BeanUtils.copyProperties(dto, warehouse);
        warehouseMapper.update(warehouse);
        cacheService.delete("cache:warehouse:" + dto.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Warehouse warehouse = warehouseMapper.findById(id);
        if (warehouse == null) {
            throw new BusinessException(ErrorCode.WAREHOUSE_NOT_FOUND);
        }
        warehouseMapper.deleteById(id);
        cacheService.delete("cache:warehouse:" + id);
    }

    private WarehouseVO convertToVO(Warehouse warehouse) {
        WarehouseVO vo = new WarehouseVO();
        BeanUtils.copyProperties(warehouse, vo);
        return vo;
    }
}
