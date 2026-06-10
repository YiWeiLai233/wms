package com.yiweilai.wms.product.service;

import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.product.dto.ProductQueryDTO;
import com.yiweilai.wms.product.dto.ProductSaveDTO;
import com.yiweilai.wms.product.vo.ProductVO;

/**
 * 商品 Service
 */
public interface ProductService {

    /**
     * 分页查询商品
     */
    PageResult<ProductVO> findByPage(ProductQueryDTO query);

    /**
     * 根据ID查询商品详情（含SKU）
     */
    ProductVO getById(Long id);

    /**
     * 新增商品
     */
    Long create(ProductSaveDTO dto);

    /**
     * 修改商品
     */
    void update(ProductSaveDTO dto);

    /**
     * 删除商品
     */
    void delete(Long id);
}
