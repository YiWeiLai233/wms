package com.yiweilai.wms.express.controller;

import com.yiweilai.wms.common.Result;
import com.yiweilai.wms.express.dto.ExpressQueryDTO;
import com.yiweilai.wms.express.service.ExpressService;
import com.yiweilai.wms.express.vo.ExpressInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 快递查询 Controller
 */
@Tag(name = "快递查询", description = "快递物流查询、费用计算")
@RestController
@RequestMapping("/api/express")
@RequiredArgsConstructor
public class ExpressController {

    private final ExpressService expressService;

    /**
     * 查询快递信息
     */
    @Operation(summary = "查询快递信息")
    @GetMapping("/query")
    public Result<ExpressInfoVO> query(@RequestParam String trackingNo,
                                        @RequestParam(required = false) String carrier) {
        ExpressInfoVO result = expressService.query(trackingNo, carrier);
        if (result == null) {
            return Result.error(404, "未查询到快递信息");
        }
        return Result.success(result);
    }

    /**
     * 计算快递费用
     */
    @Operation(summary = "计算快递费用")
    @PostMapping("/calculate-fee")
    public Result<ExpressInfoVO.FeeInfo> calculateFee(@RequestBody ExpressQueryDTO dto) {
        return Result.success(expressService.calculateFee(dto));
    }
}
