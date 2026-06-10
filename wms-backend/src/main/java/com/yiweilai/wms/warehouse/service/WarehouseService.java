package com.yiweilai.wms.warehouse.service;

import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.warehouse.dto.WarehouseQueryDTO;
import com.yiweilai.wms.warehouse.dto.WarehouseSaveDTO;
import com.yiweilai.wms.warehouse.vo.WarehouseVO;

/**
 * 仓库 Service
 */
public interface WarehouseService {

    /**
     * 分页查询仓库
     */
    PageResult<WarehouseVO> findByPage(WarehouseQueryDTO query);

    /**
     * 根据ID查询仓库详情（含库区）
     */
    WarehouseVO getById(Long id);

    /**
     * 新增仓库
     */
    Long create(WarehouseSaveDTO dto);

    /**
     * 修改仓库
     */
    void update(WarehouseSaveDTO dto);

    /**
     * 删除仓库
     */
    void delete(Long id);
}
