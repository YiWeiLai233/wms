package com.yiweilai.wms.warehouse.service;

import com.yiweilai.wms.warehouse.dto.WarehouseLocationSaveDTO;
import com.yiweilai.wms.warehouse.vo.WarehouseLocationVO;

import java.util.List;

/**
 * 库位 Service
 */
public interface WarehouseLocationService {

    /**
     * 根据货架ID查询库位列表
     */
    List<WarehouseLocationVO> findByShelfId(Long shelfId);

    /**
     * 根据ID查询库位
     */
    WarehouseLocationVO getById(Long id);

    /**
     * 新增库位
     */
    Long create(WarehouseLocationSaveDTO dto);

    /**
     * 修改库位
     */
    void update(WarehouseLocationSaveDTO dto);

    /**
     * 删除库位
     */
    void delete(Long id);
}
