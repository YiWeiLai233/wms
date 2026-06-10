package com.yiweilai.wms.product.mapper;

import com.yiweilai.wms.product.entity.ProductSku;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品SKU Mapper
 */
@Mapper
public interface ProductSkuMapper {

    /**
     * 根据商品ID查询SKU列表
     */
    List<ProductSku> findByProductId(@Param("productId") Long productId);

    /**
     * 分页查询SKU
     */
    List<ProductSku> findByPage(@Param("keyword") String keyword, @Param("status") Integer status);

    /**
     * 根据ID查询
     */
    ProductSku findById(@Param("id") Long id);

    /**
     * 根据SKU编码查询
     */
    ProductSku findBySkuCode(@Param("skuCode") String skuCode);

    /**
     * 新增
     */
    int insert(ProductSku sku);

    /**
     * 修改
     */
    int update(ProductSku sku);

    /**
     * 删除
     */
    int deleteById(@Param("id") Long id);

    /**
     * 根据商品ID删除所有SKU
     */
    int deleteByProductId(@Param("productId") Long productId);
}
