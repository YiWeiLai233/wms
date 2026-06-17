package com.yiweilai.wms.product.controller;

import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.common.Result;
import com.yiweilai.wms.product.dto.ProductBarcodeSaveDTO;
import com.yiweilai.wms.product.dto.ProductSkuQueryDTO;
import com.yiweilai.wms.product.dto.ProductSkuSaveDTO;
import com.yiweilai.wms.product.service.ProductSkuService;
import com.yiweilai.wms.product.vo.ProductSkuListVO;
import com.yiweilai.wms.product.vo.ProductSkuVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.yiweilai.wms.security.RequirePermission;

/**
 * 商品SKU Controller
 */
@RequirePermission("product.sku")
@RestController
@RequestMapping("/api/skus")
@RequiredArgsConstructor
public class ProductSkuController {

    private final ProductSkuService skuService;

    /**
     * SKU分页列表
     */
    @GetMapping
    public Result<PageResult<ProductSkuListVO>> list(ProductSkuQueryDTO query) {
        return Result.success(skuService.findByPage(query));
    }

    /**
     * 根据商品ID查询SKU列表
     */
    @GetMapping("/product/{productId}")
    public Result<List<ProductSkuVO>> listByProductId(@PathVariable Long productId) {
        return Result.success(skuService.findByProductId(productId));
    }

    /**
     * 根据ID查询SKU详情
     */
    @GetMapping("/{id}")
    public Result<ProductSkuVO> getById(@PathVariable Long id) {
        return Result.success(skuService.getById(id));
    }

    /**
     * 根据SKU编码查询
     */
    @GetMapping("/code/{skuCode}")
    public Result<ProductSkuVO> getBySkuCode(@PathVariable String skuCode) {
        return Result.success(skuService.getBySkuCode(skuCode));
    }

    /**
     * 新增SKU
     */
    @PostMapping
    public Result<Long> create(@Valid @RequestBody ProductSkuSaveDTO dto) {
        return Result.success(skuService.create(dto));
    }

    /**
     * 修改SKU
     */
    @PutMapping
    public Result<Void> update(@Valid @RequestBody ProductSkuSaveDTO dto) {
        skuService.update(dto);
        return Result.success();
    }

    /**
     * 删除SKU
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        skuService.delete(id);
        return Result.success();
    }
}
