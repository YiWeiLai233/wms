package com.yiweilai.wms.returns.controller;

import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.common.Result;
import com.yiweilai.wms.returns.dto.ReturnCheckDTO;
import com.yiweilai.wms.returns.dto.ReturnConfirmDTO;
import com.yiweilai.wms.returns.dto.ReturnCreateDTO;
import com.yiweilai.wms.returns.dto.ReturnQueryDTO;
import com.yiweilai.wms.returns.service.ReturnService;
import com.yiweilai.wms.returns.vo.ReturnOrderVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 退货 Controller
 */
@RestController
@RequestMapping("/api/returns")
@RequiredArgsConstructor
public class ReturnController {

    private final ReturnService returnService;

    /**
     * 退货单列表（分页）
     */
    @GetMapping("/list")
    public Result<PageResult<ReturnOrderVO>> list(ReturnQueryDTO query) {
        return Result.success(returnService.findByPage(query));
    }

    /**
     * 根据ID查询退货单详情
     */
    @GetMapping("/{id}")
    public Result<ReturnOrderVO> getById(@PathVariable Long id) {
        return Result.success(returnService.getById(id));
    }

    /**
     * 创建退货单
     */
    @PostMapping("/create")
    public Result<Long> create(@Valid @RequestBody ReturnCreateDTO dto) {
        return Result.success(returnService.create(dto));
    }

    /**
     * 退货质检
     */
    @PostMapping("/check")
    public Result<Void> check(@Valid @RequestBody ReturnCheckDTO dto) {
        returnService.check(dto);
        return Result.success();
    }

    /**
     * 确认退货入库
     */
    @PostMapping("/confirm")
    public Result<Void> confirm(@Valid @RequestBody ReturnConfirmDTO dto) {
        returnService.confirm(dto.getReturnId());
        return Result.success();
    }
}
