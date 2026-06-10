package com.yiweilai.wms.product.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.exception.BusinessException;
import com.yiweilai.wms.exception.ErrorCode;
import com.yiweilai.wms.product.dto.ProductQueryDTO;
import com.yiweilai.wms.product.dto.ProductSaveDTO;
import com.yiweilai.wms.product.entity.Product;
import com.yiweilai.wms.product.mapper.ProductMapper;
import com.yiweilai.wms.product.mapper.ProductSkuMapper;
import com.yiweilai.wms.product.service.ProductService;
import com.yiweilai.wms.product.vo.ProductSkuVO;
import com.yiweilai.wms.product.vo.ProductVO;
import com.yiweilai.wms.warehouse.entity.WarehouseShelf;
import com.yiweilai.wms.warehouse.mapper.WarehouseShelfMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 商品 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;
    private final ProductSkuMapper skuMapper;
    private final WarehouseShelfMapper shelfMapper;

    @Override
    public PageResult<ProductVO> findByPage(ProductQueryDTO query) {
        PageHelper.startPage(query.getPage(), query.getSize());
        List<Product> products = productMapper.findByPage(
                query.getKeyword(), query.getShelfId(), query.getStatus());

        PageInfo<Product> pageInfo = new PageInfo<>(products);

        List<ProductVO> voList = products.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        PageResult<ProductVO> result = new PageResult<>();
        result.setTotal(pageInfo.getTotal());
        result.setList(voList);
        return result;
    }

    @Override
    public ProductVO getById(Long id) {
        Product product = productMapper.findById(id);
        if (product == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "商品不存在");
        }

        ProductVO vo = convertToVO(product);

        // 查询关联的SKU
        List<ProductSkuVO> skuList = skuMapper.findByProductId(id).stream()
                .map(sku -> {
                    ProductSkuVO skuVO = new ProductSkuVO();
                    BeanUtils.copyProperties(sku, skuVO);
                    skuVO.setProductName(product.getName());
                    return skuVO;
                })
                .collect(Collectors.toList());
        vo.setSkuList(skuList);

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ProductSaveDTO dto) {
        // 检查SPU编码唯一性
        Product existing = productMapper.findBySpuCode(dto.getSpuCode());
        if (existing != null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "SPU编码已存在");
        }

        Product product = new Product();
        BeanUtils.copyProperties(dto, product);
        productMapper.insert(product);
        return product.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(ProductSaveDTO dto) {
        Product product = productMapper.findById(dto.getId());
        if (product == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "商品不存在");
        }

        // 检查SPU编码唯一性（排除自身）
        if (!product.getSpuCode().equals(dto.getSpuCode())) {
            Product existing = productMapper.findBySpuCode(dto.getSpuCode());
            if (existing != null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "SPU编码已存在");
            }
        }

        BeanUtils.copyProperties(dto, product);
        productMapper.update(product);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Product product = productMapper.findById(id);
        if (product == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "商品不存在");
        }
        productMapper.deleteById(id);
    }

    private ProductVO convertToVO(Product product) {
        ProductVO vo = new ProductVO();
        BeanUtils.copyProperties(product, vo);

        // 查询货架信息
        if (product.getShelfId() != null) {
            WarehouseShelf shelf = shelfMapper.findById(product.getShelfId());
            if (shelf != null) {
                vo.setShelfCode(shelf.getCode());
                vo.setCategoryName(shelf.getCategoryName());
            }
        }
        return vo;
    }
}
