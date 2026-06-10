package com.yiweilai.wms.product.service;

import com.yiweilai.wms.product.dto.ProductCategorySaveDTO;
import com.yiweilai.wms.product.vo.ProductCategoryVO;

import java.util.List;

/**
 * 商品分类 Service
 */
public interface ProductCategoryService {

    /**
     * 获取分类树
     */
    List<ProductCategoryVO> getCategoryTree();

    /**
     * 根据ID查询
     */
    ProductCategoryVO getById(Long id);

    /**
     * 新增分类
     */
    Long create(ProductCategorySaveDTO dto);

    /**
     * 修改分类
     */
    void update(ProductCategorySaveDTO dto);

    /**
     * 删除分类
     */
    void delete(Long id);
}
