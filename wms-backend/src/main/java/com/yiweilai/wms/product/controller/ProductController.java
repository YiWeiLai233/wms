package com.yiweilai.wms.product.controller;

import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.common.Result;
import com.yiweilai.wms.product.dto.ProductQueryDTO;
import com.yiweilai.wms.product.dto.ProductSaveDTO;
import com.yiweilai.wms.product.service.ProductService;
import com.yiweilai.wms.product.vo.ProductVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.yiweilai.wms.security.RequirePermission;

/**
 * 商品 Controller
 */
@RequirePermission("product.list")
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * 商品列表（分页）
     */
    @GetMapping
    public Result<PageResult<ProductVO>> list(ProductQueryDTO query) {
        return Result.success(productService.findByPage(query));
    }

    /**
     * 根据ID查询商品详情
     */
    @GetMapping("/{id}")
    public Result<ProductVO> getById(@PathVariable Long id) {
        return Result.success(productService.getById(id));
    }

    /**
     * 新增商品
     */
    @PostMapping
    public Result<Long> create(@Valid @RequestBody ProductSaveDTO dto) {
        return Result.success(productService.create(dto));
    }

    /**
     * 修改商品
     */
    @PutMapping
    public Result<Void> update(@Valid @RequestBody ProductSaveDTO dto) {
        productService.update(dto);
        return Result.success();
    }

    /**
     * 删除商品
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return Result.success();
    }
}
