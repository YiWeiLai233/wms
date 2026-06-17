package com.yiweilai.wms.product.service.impl;

import com.yiweilai.wms.config.CacheService;
import com.yiweilai.wms.exception.BusinessException;
import com.yiweilai.wms.exception.ErrorCode;
import com.yiweilai.wms.product.dto.ProductCategorySaveDTO;
import com.yiweilai.wms.product.entity.ProductCategory;
import com.yiweilai.wms.product.mapper.ProductCategoryMapper;
import com.yiweilai.wms.product.service.ProductCategoryService;
import com.yiweilai.wms.product.vo.ProductCategoryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 商品分类 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductCategoryServiceImpl implements ProductCategoryService {

    private final ProductCategoryMapper categoryMapper;
    private final CacheService cacheService;

    private static final String CACHE_KEY_CATEGORY_TREE = "cache:category:tree";

    @Override
    @SuppressWarnings("unchecked")
    public List<ProductCategoryVO> getCategoryTree() {
        List<ProductCategoryVO> cached = cacheService.get(CACHE_KEY_CATEGORY_TREE);
        if (cached != null) {
            return cached;
        }
        List<ProductCategory> allCategories = categoryMapper.findAll();
        List<ProductCategoryVO> tree = buildTree(allCategories, 0L);
        cacheService.set(CACHE_KEY_CATEGORY_TREE, tree, 30, TimeUnit.MINUTES);
        return tree;
    }

    @Override
    public ProductCategoryVO getById(Long id) {
        ProductCategory category = categoryMapper.findById(id);
        if (category == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "分类不存在");
        }
        return convertToVO(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ProductCategorySaveDTO dto) {
        ProductCategory category = new ProductCategory();
        BeanUtils.copyProperties(dto, category);
        categoryMapper.insert(category);
        cacheService.delete(CACHE_KEY_CATEGORY_TREE);
        return category.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(ProductCategorySaveDTO dto) {
        ProductCategory category = categoryMapper.findById(dto.getId());
        if (category == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "分类不存在");
        }
        BeanUtils.copyProperties(dto, category);
        categoryMapper.update(category);
        cacheService.delete(CACHE_KEY_CATEGORY_TREE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        // 检查是否有子分类
        List<ProductCategory> children = categoryMapper.findByParentId(id);
        if (children != null && !children.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "存在子分类，不能删除");
        }
        categoryMapper.deleteById(id);
        cacheService.delete(CACHE_KEY_CATEGORY_TREE);
    }

    /**
     * 构建分类树
     */
    private List<ProductCategoryVO> buildTree(List<ProductCategory> allCategories, Long parentId) {
        Map<Long, List<ProductCategory>> parentMap = allCategories.stream()
                .collect(Collectors.groupingBy(ProductCategory::getParentId));

        return buildChildren(parentMap, parentId);
    }

    private List<ProductCategoryVO> buildChildren(Map<Long, List<ProductCategory>> parentMap, Long parentId) {
        List<ProductCategory> children = parentMap.getOrDefault(parentId, new ArrayList<>());
        List<ProductCategoryVO> voList = new ArrayList<>();

        for (ProductCategory category : children) {
            ProductCategoryVO vo = convertToVO(category);
            vo.setChildren(buildChildren(parentMap, category.getId()));
            voList.add(vo);
        }
        return voList;
    }

    private ProductCategoryVO convertToVO(ProductCategory category) {
        ProductCategoryVO vo = new ProductCategoryVO();
        BeanUtils.copyProperties(category, vo);
        return vo;
    }
}
