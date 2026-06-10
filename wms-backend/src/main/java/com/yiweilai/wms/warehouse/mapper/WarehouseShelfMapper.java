package com.yiweilai.wms.warehouse.mapper;

import com.yiweilai.wms.warehouse.entity.WarehouseShelf;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 货架 Mapper
 */
@Mapper
public interface WarehouseShelfMapper {

    /**
     * 根据仓库ID查询货架列表
     */
    List<WarehouseShelf> findByWarehouseId(@Param("warehouseId") Long warehouseId);

    /**
     * 根据ID查询
     */
    WarehouseShelf findById(@Param("id") Long id);

    /**
     * 根据仓库ID和编码查询
     */
    WarehouseShelf findByWarehouseIdAndCode(@Param("warehouseId") Long warehouseId,
                                           @Param("code") String code);

    /**
     * 新增
     */
    int insert(WarehouseShelf shelf);

    /**
     * 修改
     */
    int update(WarehouseShelf shelf);

    /**
     * 删除
     */
    int deleteById(@Param("id") Long id);

    /**
     * 根据仓库ID删除所有货架
     */
    int deleteByWarehouseId(@Param("warehouseId") Long warehouseId);
}
