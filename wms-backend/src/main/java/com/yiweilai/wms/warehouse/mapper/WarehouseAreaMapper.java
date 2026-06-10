package com.yiweilai.wms.warehouse.mapper;

import com.yiweilai.wms.warehouse.entity.WarehouseArea;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 库区 Mapper
 */
@Mapper
public interface WarehouseAreaMapper {

    /**
     * 根据仓库ID查询库区列表
     */
    List<WarehouseArea> findByWarehouseId(@Param("warehouseId") Long warehouseId);

    /**
     * 根据ID查询
     */
    WarehouseArea findById(@Param("id") Long id);

    /**
     * 根据仓库ID和编码查询
     */
    WarehouseArea findByWarehouseIdAndCode(@Param("warehouseId") Long warehouseId,
                                           @Param("code") String code);

    /**
     * 新增
     */
    int insert(WarehouseArea area);

    /**
     * 修改
     */
    int update(WarehouseArea area);

    /**
     * 删除
     */
    int deleteById(@Param("id") Long id);

    /**
     * 根据仓库ID删除所有库区
     */
    int deleteByWarehouseId(@Param("warehouseId") Long warehouseId);
}
