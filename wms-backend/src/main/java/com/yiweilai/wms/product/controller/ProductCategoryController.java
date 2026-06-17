package com.yiweilai.wms.product.controller;

import com.yiweilai.wms.common.Result;
import com.yiweilai.wms.product.dto.ProductCategorySaveDTO;
import com.yiweilai.wms.product.service.ProductCategoryService;
import com.yiweilai.wms.product.vo.ProductCategoryVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.yiweilai.wms.security.RequirePermission;

/**
 * 商品分类 Controller
 */
@RequirePermission("product.list")
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class ProductCategoryController {

    private final ProductCategoryService categoryService;

    /**
     * 获取分类树
     */
    @GetMapping("/tree")
    public Result<List<ProductCategoryVO>> getCategoryTree() {
        return Result.success(categoryService.getCategoryTree());
    }

    /**
     * 根据ID查询分类
     */
    @GetMapping("/{id}")
    public Result<ProductCategoryVO> getById(@PathVariable Long id) {
        return Result.success(categoryService.getById(id));
    }

    /**
     * 新增分类
     */
    @PostMapping
    public Result<Long> create(@Valid @RequestBody ProductCategorySaveDTO dto) {
        return Result.success(categoryService.create(dto));
    }

    /**
     * 修改分类
     */
    @PutMapping
    public Result<Void> update(@Valid @RequestBody ProductCategorySaveDTO dto) {
        categoryService.update(dto);
        return Result.success();
    }

    /**
     * 删除分类
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.success();
    }
}
