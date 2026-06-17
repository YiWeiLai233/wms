package com.yiweilai.wms.alert.controller;

import com.yiweilai.wms.alert.dto.StockAlertTemplateCreateDTO;
import com.yiweilai.wms.alert.dto.StockAlertTemplateUpdateDTO;
import com.yiweilai.wms.alert.service.StockAlertTemplateService;
import com.yiweilai.wms.alert.vo.StockAlertTemplateVO;
import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.common.Result;
import jakarta.validation.Valid;
import com.yiweilai.wms.security.RequirePermission;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 预警模板 Controller
 */
@RequirePermission("system")
@RestController
@RequestMapping("/api/stock-alert-templates")
@RequiredArgsConstructor
public class StockAlertTemplateController {

    private final StockAlertTemplateService templateService;

    /**
     * 分页查询模板列表
     */
    @GetMapping
    public Result<PageResult<StockAlertTemplateVO>> query(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer enabled,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(templateService.findByPage(keyword, enabled, page, size));
    }

    /**
     * 根据ID查询模板详情
     */
    @GetMapping("/{id}")
    public Result<StockAlertTemplateVO> getById(@PathVariable Long id) {
        return Result.success(templateService.getById(id));
    }

    /**
     * 查询启用的模板列表（用于下拉选择）
     */
    @GetMapping("/options")
    public Result<List<StockAlertTemplateVO>> getOptions() {
        return Result.success(templateService.findEnabledList());
    }

    /**
     * 创建模板
     */
    @PostMapping
    public Result<Long> create(@Valid @RequestBody StockAlertTemplateCreateDTO dto) {
        return Result.success(templateService.create(dto));
    }

    /**
     * 更新模板
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody StockAlertTemplateUpdateDTO dto) {
        dto.setId(id);
        templateService.update(dto);
        return Result.success();
    }

    /**
     * 删除模板
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        templateService.delete(id);
        return Result.success();
    }
}
