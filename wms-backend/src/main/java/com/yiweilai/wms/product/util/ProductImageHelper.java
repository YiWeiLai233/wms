package com.yiweilai.wms.product.util;

import com.yiweilai.wms.product.entity.Product;
import com.yiweilai.wms.product.entity.ProductSku;
import com.yiweilai.wms.product.mapper.ProductMapper;
import com.yiweilai.wms.product.mapper.ProductSkuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 商品图片工具类
 * 优先返回SKU图片，无SKU图片时回退到商品主图
 */
@Component
@RequiredArgsConstructor
public class ProductImageHelper {

    private final ProductSkuMapper productSkuMapper;
    private final ProductMapper productMapper;

    /**
     * 获取SKU图片，若SKU无图片则回退到商品主图
     *
     * @param skuId SKU ID
     * @return 图片URL，无图片时返回null
     */
    public String getSkuImage(Long skuId) {
        if (skuId == null) {
            return null;
        }

        ProductSku sku = productSkuMapper.findById(skuId);
        if (sku == null) {
            return null;
        }

        // 优先返回SKU图片
        if (StringUtils.hasText(sku.getImage())) {
            return sku.getImage();
        }

        // 回退到商品主图
        if (sku.getProductId() != null) {
            Product product = productMapper.findById(sku.getProductId());
            if (product != null && StringUtils.hasText(product.getMainImage())) {
                return product.getMainImage();
            }
        }

        return null;
    }
}
