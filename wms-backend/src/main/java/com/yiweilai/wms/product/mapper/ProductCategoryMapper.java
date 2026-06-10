package com.yiweilai.wms.product.mapper;

import com.yiweilai.wms.product.entity.ProductCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品分类 Mapper
 */
@Mapper
public interface ProductCategoryMapper {

    /**
     * 查询所有分类
     */
    List<ProductCategory> findAll();

    /**
     * 根据ID查询
     */
    ProductCategory findById(@Param("id") Long id);

    /**
     * 根据父ID查询子分类
     */
    List<ProductCategory> findByParentId(@Param("parentId") Long parentId);

    /**
     * 新增
     */
    int insert(ProductCategory category);

    /**
     * 修改
     */
    int update(ProductCategory category);

    /**
     * 删除
     */
    int deleteById(@Param("id") Long id);
}
