package com.yiweilai.wms.express.controller;

import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.common.Result;
import com.yiweilai.wms.express.dto.ExpressCompanySaveDTO;
import com.yiweilai.wms.express.service.ExpressCompanyService;
import com.yiweilai.wms.express.vo.ExpressCompanyVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 快递公司 Controller
 */
@Tag(name = "快递公司", description = "快递公司管理")
@RestController
@RequestMapping("/api/express/companies")
@RequiredArgsConstructor
public class ExpressCompanyController {

    private final ExpressCompanyService companyService;

    /**
     * 查询所有启用的公司（用于下拉选择）
     */
    @Operation(summary = "公司列表（下拉选择）")
    @GetMapping
    public Result<List<ExpressCompanyVO>> findAll() {
        return Result.success(companyService.findAll());
    }

    /**
     * 分页查询
     */
    @Operation(summary = "公司分页列表")
    @GetMapping("/page")
    public Result<PageResult<ExpressCompanyVO>> findByPage(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(companyService.findByPage(name, status, page, size));
    }

    /**
     * 根据ID查询
     */
    @Operation(summary = "公司详情")
    @GetMapping("/{id}")
    public Result<ExpressCompanyVO> getById(@PathVariable Long id) {
        return Result.success(companyService.getById(id));
    }

    /**
     * 新增
     */
    @Operation(summary = "新增公司")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody ExpressCompanySaveDTO dto) {
        return Result.success(companyService.create(dto));
    }

    /**
     * 修改
     */
    @Operation(summary = "修改公司")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody ExpressCompanySaveDTO dto) {
        companyService.update(dto);
        return Result.success();
    }

    /**
     * 删除
     */
    @Operation(summary = "删除公司")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        companyService.delete(id);
        return Result.success();
    }
}
