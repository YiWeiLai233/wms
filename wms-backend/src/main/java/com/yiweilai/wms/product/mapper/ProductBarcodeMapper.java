package com.yiweilai.wms.product.mapper;

import com.yiweilai.wms.product.entity.ProductBarcode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品条码 Mapper
 */
@Mapper
public interface ProductBarcodeMapper {

    /**
     * 根据SKU ID查询条码列表
     */
    List<ProductBarcode> findBySkuId(@Param("skuId") Long skuId);

    /**
     * 根据条码查询
     */
    ProductBarcode findByBarcode(@Param("barcode") String barcode);

    /**
     * 新增
     */
    int insert(ProductBarcode barcode);

    /**
     * 删除
     */
    int deleteById(@Param("id") Long id);

    /**
     * 根据SKU ID删除所有条码
     */
    int deleteBySkuId(@Param("skuId") Long skuId);
}
