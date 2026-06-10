package com.yiweilai.wms.warehouse.mapper;

import com.yiweilai.wms.warehouse.entity.WarehouseLocation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 库位 Mapper
 */
@Mapper
public interface WarehouseLocationMapper {

    /**
     * 根据货架ID查询库位列表
     */
    List<WarehouseLocation> findByShelfId(@Param("shelfId") Long shelfId);

    /**
     * 根据ID查询
     */
    WarehouseLocation findById(@Param("id") Long id);

    /**
     * 根据货架ID和编码查询
     */
    WarehouseLocation findByShelfIdAndCode(@Param("shelfId") Long shelfId,
                                           @Param("code") String code);

    /**
     * 新增
     */
    int insert(WarehouseLocation location);

    /**
     * 修改
     */
    int update(WarehouseLocation location);

    /**
     * 删除
     */
    int deleteById(@Param("id") Long id);

    /**
     * 根据货架ID删除所有库位
     */
    int deleteByShelfId(@Param("shelfId") Long shelfId);
}
