package com.yiweilai.wms.platform.controller;

import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.common.Result;
import com.yiweilai.wms.platform.dto.PlatformCreateDTO;
import com.yiweilai.wms.platform.dto.PlatformUpdateDTO;
import com.yiweilai.wms.platform.service.PlatformService;
import com.yiweilai.wms.platform.vo.PlatformVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 平台 Controller
 */
@RestController
@RequestMapping("/api/platforms")
@RequiredArgsConstructor
public class PlatformController {

    private final PlatformService platformService;

    /**
     * 分页查询平台列表
     */
    @GetMapping
    public Result<PageResult<PlatformVO>> query(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer enabled,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(platformService.findByPage(keyword, enabled, page, size));
    }

    /**
     * 根据ID查询平台详情
     */
    @GetMapping("/{id}")
    public Result<PlatformVO> getById(@PathVariable Long id) {
        return Result.success(platformService.getById(id));
    }

    /**
     * 查询启用的平台列表（用于下拉选择）
     */
    @GetMapping("/options")
    public Result<List<PlatformVO>> getOptions() {
        return Result.success(platformService.findEnabledList());
    }

    /**
     * 创建平台
     */
    @PostMapping
    public Result<Long> create(@Valid @RequestBody PlatformCreateDTO dto) {
        return Result.success(platformService.create(dto));
    }

    /**
     * 更新平台
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody PlatformUpdateDTO dto) {
        dto.setId(id);
        platformService.update(dto);
        return Result.success();
    }

    /**
     * 删除平台
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        platformService.delete(id);
        return Result.success();
    }
}
