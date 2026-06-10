package com.yiweilai.wms.warehouse.service.impl;

import com.yiweilai.wms.exception.BusinessException;
import com.yiweilai.wms.exception.ErrorCode;
import com.yiweilai.wms.warehouse.dto.WarehouseLocationSaveDTO;
import com.yiweilai.wms.warehouse.entity.WarehouseLocation;
import com.yiweilai.wms.warehouse.mapper.WarehouseLocationMapper;
import com.yiweilai.wms.warehouse.service.WarehouseLocationService;
import com.yiweilai.wms.warehouse.vo.WarehouseLocationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 库位 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseLocationServiceImpl implements WarehouseLocationService {

    private final WarehouseLocationMapper locationMapper;

    @Override
    public List<WarehouseLocationVO> findByShelfId(Long shelfId) {
        return locationMapper.findByShelfId(shelfId).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public WarehouseLocationVO getById(Long id) {
        WarehouseLocation location = locationMapper.findById(id);
        if (location == null) {
            throw new BusinessException(ErrorCode.LOCATION_NOT_FOUND);
        }
        return convertToVO(location);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(WarehouseLocationSaveDTO dto) {
        // 检查货架内库位编码唯一性
        WarehouseLocation existing = locationMapper.findByShelfIdAndCode(dto.getShelfId(), dto.getCode());
        if (existing != null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "库位编码已存在");
        }

        WarehouseLocation location = new WarehouseLocation();
        BeanUtils.copyProperties(dto, location);
        locationMapper.insert(location);
        return location.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(WarehouseLocationSaveDTO dto) {
        WarehouseLocation location = locationMapper.findById(dto.getId());
        if (location == null) {
            throw new BusinessException(ErrorCode.LOCATION_NOT_FOUND);
        }

        // 检查编码唯一性（排除自身）
        if (!location.getCode().equals(dto.getCode())) {
            WarehouseLocation existing = locationMapper.findByShelfIdAndCode(dto.getShelfId(), dto.getCode());
            if (existing != null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "库位编码已存在");
            }
        }

        BeanUtils.copyProperties(dto, location);
        locationMapper.update(location);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        WarehouseLocation location = locationMapper.findById(id);
        if (location == null) {
            throw new BusinessException(ErrorCode.LOCATION_NOT_FOUND);
        }
        locationMapper.deleteById(id);
    }

    private WarehouseLocationVO convertToVO(WarehouseLocation location) {
        WarehouseLocationVO vo = new WarehouseLocationVO();
        BeanUtils.copyProperties(location, vo);
        return vo;
    }
}
