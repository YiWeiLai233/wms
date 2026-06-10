package com.yiweilai.wms.warehouse.service.impl;

import com.yiweilai.wms.exception.BusinessException;
import com.yiweilai.wms.exception.ErrorCode;
import com.yiweilai.wms.warehouse.dto.WarehouseAreaSaveDTO;
import com.yiweilai.wms.warehouse.entity.WarehouseArea;
import com.yiweilai.wms.warehouse.mapper.WarehouseAreaMapper;
import com.yiweilai.wms.warehouse.mapper.WarehouseShelfMapper;
import com.yiweilai.wms.warehouse.service.WarehouseAreaService;
import com.yiweilai.wms.warehouse.vo.WarehouseAreaVO;
import com.yiweilai.wms.warehouse.vo.WarehouseShelfVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 库区 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseAreaServiceImpl implements WarehouseAreaService {

    private final WarehouseAreaMapper areaMapper;
    private final WarehouseShelfMapper shelfMapper;

    @Override
    public List<WarehouseAreaVO> findByWarehouseId(Long warehouseId) {
        return areaMapper.findByWarehouseId(warehouseId).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public WarehouseAreaVO getById(Long id) {
        WarehouseArea area = areaMapper.findById(id);
        if (area == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "库区不存在");
        }

        WarehouseAreaVO vo = convertToVO(area);

        // 货架已改为直接关联仓库，不再通过库区查询
        vo.setShelfList(new java.util.ArrayList<>());

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(WarehouseAreaSaveDTO dto) {
        // 检查仓库内库区编码唯一性
        WarehouseArea existing = areaMapper.findByWarehouseIdAndCode(dto.getWarehouseId(), dto.getCode());
        if (existing != null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "库区编码已存在");
        }

        WarehouseArea area = new WarehouseArea();
        BeanUtils.copyProperties(dto, area);
        areaMapper.insert(area);
        return area.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(WarehouseAreaSaveDTO dto) {
        WarehouseArea area = areaMapper.findById(dto.getId());
        if (area == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "库区不存在");
        }

        // 检查编码唯一性（排除自身）
        if (!area.getCode().equals(dto.getCode())) {
            WarehouseArea existing = areaMapper.findByWarehouseIdAndCode(dto.getWarehouseId(), dto.getCode());
            if (existing != null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "库区编码已存在");
            }
        }

        BeanUtils.copyProperties(dto, area);
        areaMapper.update(area);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        WarehouseArea area = areaMapper.findById(id);
        if (area == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "库区不存在");
        }
        areaMapper.deleteById(id);
    }

    private WarehouseAreaVO convertToVO(WarehouseArea area) {
        WarehouseAreaVO vo = new WarehouseAreaVO();
        BeanUtils.copyProperties(area, vo);
        return vo;
    }
}
