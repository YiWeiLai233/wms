package com.yiweilai.wms.warehouse.mapper;

import com.yiweilai.wms.warehouse.entity.Warehouse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 仓库 Mapper
 */
@Mapper
public interface WarehouseMapper {

    /**
     * 分页查询
     */
    List<Warehouse> findByPage(@Param("keyword") String keyword,
                               @Param("status") Integer status);

    /**
     * 根据ID查询
     */
    Warehouse findById(@Param("id") Long id);

    /**
     * 根据编码查询
     */
    Warehouse findByCode(@Param("code") String code);

    /**
     * 新增
     */
    int insert(Warehouse warehouse);

    /**
     * 修改
     */
    int update(Warehouse warehouse);

    /**
     * 删除
     */
    int deleteById(@Param("id") Long id);
}
