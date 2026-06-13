package com.yiweilai.wms.express.controller;

import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.common.Result;
import com.yiweilai.wms.express.dto.ExpressFeeTemplateSaveDTO;
import com.yiweilai.wms.express.service.ExpressFeeTemplateService;
import com.yiweilai.wms.express.vo.ExpressFeeTemplateVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 快递费用模板 Controller
 */
@Tag(name = "快递费用模板", description = "快递费用模板管理")
@RestController
@RequestMapping("/api/express/fee-templates")
@RequiredArgsConstructor
public class ExpressFeeTemplateController {

    private final ExpressFeeTemplateService templateService;

    /**
     * 根据公司ID查询模板列表
     */
    @Operation(summary = "公司下的模板列表")
    @GetMapping("/company/{companyId}")
    public Result<List<ExpressFeeTemplateVO>> findByCompanyId(@PathVariable Long companyId) {
        return Result.success(templateService.findByCompanyId(companyId));
    }

    /**
     * 分页查询
     */
    @Operation(summary = "模板分页列表")
    @GetMapping
    public Result<PageResult<ExpressFeeTemplateVO>> findByPage(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(templateService.findByPage(companyId, name, page, size));
    }

    /**
     * 根据ID查询详情
     */
    @Operation(summary = "模板详情")
    @GetMapping("/{id}")
    public Result<ExpressFeeTemplateVO> getById(@PathVariable Long id) {
        return Result.success(templateService.getById(id));
    }

    /**
     * 查询默认模板
     */
    @Operation(summary = "默认模板")
    @GetMapping("/default")
    public Result<ExpressFeeTemplateVO> getDefault() {
        return Result.success(templateService.getDefault());
    }

    /**
     * 新增
     */
    @Operation(summary = "新增模板")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody ExpressFeeTemplateSaveDTO dto) {
        return Result.success(templateService.create(dto));
    }

    /**
     * 修改
     */
    @Operation(summary = "修改模板")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody ExpressFeeTemplateSaveDTO dto) {
        templateService.update(dto);
        return Result.success();
    }

    /**
     * 删除
     */
    @Operation(summary = "删除模板")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        templateService.delete(id);
        return Result.success();
    }
}
