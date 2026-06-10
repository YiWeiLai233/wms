package com.yiweilai.wms.product.service;

import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.product.dto.ProductSkuQueryDTO;
import com.yiweilai.wms.product.dto.ProductSkuSaveDTO;
import com.yiweilai.wms.product.vo.ProductSkuListVO;
import com.yiweilai.wms.product.vo.ProductSkuVO;

import java.util.List;

/**
 * 商品SKU Service
 */
public interface ProductSkuService {

    /**
     * 根据商品ID查询SKU列表
     */
    List<ProductSkuVO> findByProductId(Long productId);

    /**
     * SKU分页列表（含商品名和分类名）
     */
    PageResult<ProductSkuListVO> findByPage(ProductSkuQueryDTO query);

    /**
     * 根据ID查询SKU详情（含条码）
     */
    ProductSkuVO getById(Long id);

    /**
     * 根据SKU编码查询
     */
    ProductSkuVO getBySkuCode(String skuCode);

    /**
     * 新增SKU
     */
    Long create(ProductSkuSaveDTO dto);

    /**
     * 修改SKU
     */
    void update(ProductSkuSaveDTO dto);

    /**
     * 删除SKU
     */
    void delete(Long id);
}
