package com.yiweilai.wms.warehouse.service;

import com.yiweilai.wms.warehouse.dto.WarehouseAreaSaveDTO;
import com.yiweilai.wms.warehouse.vo.WarehouseAreaVO;

import java.util.List;

/**
 * 库区 Service
 */
public interface WarehouseAreaService {

    /**
     * 根据仓库ID查询库区列表
     */
    List<WarehouseAreaVO> findByWarehouseId(Long warehouseId);

    /**
     * 根据ID查询库区详情（含货架）
     */
    WarehouseAreaVO getById(Long id);

    /**
     * 新增库区
     */
    Long create(WarehouseAreaSaveDTO dto);

    /**
     * 修改库区
     */
    void update(WarehouseAreaSaveDTO dto);

    /**
     * 删除库区
     */
    void delete(Long id);
}
