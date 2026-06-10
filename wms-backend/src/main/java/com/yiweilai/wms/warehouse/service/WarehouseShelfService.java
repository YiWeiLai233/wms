package com.yiweilai.wms.warehouse.service;

import com.yiweilai.wms.warehouse.dto.WarehouseShelfSaveDTO;
import com.yiweilai.wms.warehouse.vo.WarehouseShelfVO;

import java.util.List;

/**
 * 货架 Service
 */
public interface WarehouseShelfService {

    /**
     * 根据仓库ID查询货架列表
     */
    List<WarehouseShelfVO> findByWarehouseId(Long warehouseId);

    /**
     * 根据ID查询货架详情（含库位）
     */
    WarehouseShelfVO getById(Long id);

    /**
     * 新增货架
     */
    Long create(WarehouseShelfSaveDTO dto);

    /**
     * 修改货架
     */
    void update(WarehouseShelfSaveDTO dto);

    /**
     * 删除货架
     */
    void delete(Long id);
}
