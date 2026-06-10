package com.yiweilai.wms.warehouse.service.impl;

import com.yiweilai.wms.exception.BusinessException;
import com.yiweilai.wms.exception.ErrorCode;
import com.yiweilai.wms.warehouse.dto.WarehouseShelfSaveDTO;
import com.yiweilai.wms.warehouse.entity.WarehouseShelf;
import com.yiweilai.wms.warehouse.mapper.WarehouseLocationMapper;
import com.yiweilai.wms.warehouse.mapper.WarehouseShelfMapper;
import com.yiweilai.wms.warehouse.service.WarehouseShelfService;
import com.yiweilai.wms.warehouse.vo.WarehouseLocationVO;
import com.yiweilai.wms.warehouse.vo.WarehouseShelfVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 货架 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseShelfServiceImpl implements WarehouseShelfService {

    private final WarehouseShelfMapper shelfMapper;
    private final WarehouseLocationMapper locationMapper;

    @Override
    public List<WarehouseShelfVO> findByWarehouseId(Long warehouseId) {
        return shelfMapper.findByWarehouseId(warehouseId).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public WarehouseShelfVO getById(Long id) {
        WarehouseShelf shelf = shelfMapper.findById(id);
        if (shelf == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "货架不存在");
        }

        WarehouseShelfVO vo = convertToVO(shelf);

        // 查询关联的库位
        List<WarehouseLocationVO> locationList = locationMapper.findByShelfId(id).stream()
                .map(location -> {
                    WarehouseLocationVO locationVO = new WarehouseLocationVO();
                    BeanUtils.copyProperties(location, locationVO);
                    return locationVO;
                })
                .collect(Collectors.toList());
        vo.setLocationList(locationList);

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(WarehouseShelfSaveDTO dto) {
        // 检查仓库内货架编码唯一性
        WarehouseShelf existing = shelfMapper.findByWarehouseIdAndCode(dto.getWarehouseId(), dto.getCode());
        if (existing != null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "货架编码已存在");
        }

        WarehouseShelf shelf = new WarehouseShelf();
        BeanUtils.copyProperties(dto, shelf);
        shelfMapper.insert(shelf);
        return shelf.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(WarehouseShelfSaveDTO dto) {
        WarehouseShelf shelf = shelfMapper.findById(dto.getId());
        if (shelf == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "货架不存在");
        }

        // 检查编码唯一性（排除自身）
        if (!shelf.getCode().equals(dto.getCode())) {
            WarehouseShelf existing = shelfMapper.findByWarehouseIdAndCode(dto.getWarehouseId(), dto.getCode());
            if (existing != null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "货架编码已存在");
            }
        }

        BeanUtils.copyProperties(dto, shelf);
        shelfMapper.update(shelf);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        WarehouseShelf shelf = shelfMapper.findById(id);
        if (shelf == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "货架不存在");
        }
        shelfMapper.deleteById(id);
    }

    private WarehouseShelfVO convertToVO(WarehouseShelf shelf) {
        WarehouseShelfVO vo = new WarehouseShelfVO();
        BeanUtils.copyProperties(shelf, vo);
        return vo;
    }
}
