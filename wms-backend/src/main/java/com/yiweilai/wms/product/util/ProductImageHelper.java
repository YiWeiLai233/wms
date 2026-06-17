package com.yiweilai.wms.product.util;

import com.yiweilai.wms.product.entity.Product;
import com.yiweilai.wms.product.entity.ProductSku;
import com.yiweilai.wms.product.mapper.ProductMapper;
import com.yiweilai.wms.product.mapper.ProductSkuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 商品图片查询辅助工具
 * 统一处理 SKU 图片和 SPU 主图的查询逻辑
 */
@Component
@RequiredArgsConstructor
public class ProductImageHelper {

    private final ProductSkuMapper productSkuMapper;
    private final ProductMapper productMapper;

    /**
     * 获取 SKU 图片，如果 SKU 没有图片则返回 SPU 主图
     * @param skuId SKU ID
     * @return 图片 URL，如果没有则返回 null
     */
    public String getSkuImage(Long skuId) {
        if (skuId == null) {
            return null;
        }

        ProductSku sku = productSkuMapper.findById(skuId);
        if (sku == null) {
            return null;
        }

        return getSkuImage(sku);
    }

    /**
     * 获取 SKU 图片（通过 SKU 对象）
     * @param sku SKU 对象
     * @return 图片 URL
     */
    public String getSkuImage(ProductSku sku) {
        if (sku == null) {
            return null;
        }

        // 优先返回 SKU 图片
        String image = sku.getImage();
        if (image != null && !image.isEmpty()) {
            return image;
        }

        // SKU 没有图片，查询 SPU 主图
        if (sku.getProductId() != null) {
            Product product = productMapper.findById(sku.getProductId());
            if (product != null) {
                return product.getMainImage();
            }
        }

        return null;
    }
}
