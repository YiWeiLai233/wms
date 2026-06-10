package com.yiweilai.wms.product.mapper;

import com.yiweilai.wms.product.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品 Mapper
 */
@Mapper
public interface ProductMapper {

    /**
     * 分页查询
     */
    List<Product> findByPage(@Param("keyword") String keyword,
                             @Param("shelfId") Long shelfId,
                             @Param("status") Integer status);

    /**
     * 根据ID查询
     */
    Product findById(@Param("id") Long id);

    /**
     * 根据SPU编码查询
     */
    Product findBySpuCode(@Param("spuCode") String spuCode);

    /**
     * 新增
     */
    int insert(Product product);

    /**
     * 修改
     */
    int update(Product product);

    /**
     * 删除
     */
    int deleteById(@Param("id") Long id);
}
