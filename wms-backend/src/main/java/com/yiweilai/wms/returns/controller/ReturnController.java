package com.yiweilai.wms.returns.controller;

import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.common.Result;
import com.yiweilai.wms.returns.dto.ReturnBatchCreateDTO;
import com.yiweilai.wms.returns.dto.ReturnCheckDTO;
import com.yiweilai.wms.returns.dto.ReturnConfirmDTO;
import com.yiweilai.wms.returns.dto.ReturnCreateDTO;
import com.yiweilai.wms.returns.dto.ReturnQueryDTO;
import com.yiweilai.wms.returns.service.ReturnService;
import com.yiweilai.wms.returns.vo.ReturnOrderVO;
import com.yiweilai.wms.platform.entity.Platform;
import com.yiweilai.wms.platform.mapper.PlatformMapper;
import com.yiweilai.wms.express.entity.ExpressFeeTemplate;
import com.yiweilai.wms.express.mapper.ExpressFeeStepMapper;
import com.yiweilai.wms.express.mapper.ExpressFeeTemplateMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 退货 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/returns")
@RequiredArgsConstructor
public class ReturnController {

    private final ReturnService returnService;
    private final PlatformMapper platformMapper;
    private final ExpressFeeTemplateMapper feeTemplateMapper;
    private final ExpressFeeStepMapper feeStepMapper;
    private final com.yiweilai.wms.order.mapper.SalesOrderMapper orderMapper;
    private final com.yiweilai.wms.order.mapper.SalesOrderItemMapper orderItemMapper;
    private final com.yiweilai.wms.express.mapper.ExpressCompanyMapper expressCompanyMapper;

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
     * 批量创建退货单
     */
    @PostMapping("/create-batch")
    public Result<List<Long>> createBatch(@Valid @RequestBody ReturnBatchCreateDTO dto) {
        return Result.success(returnService.createBatch(dto));
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
        returnService.confirm(dto.getReturnId(), dto.getItems());
        return Result.success();
    }

    /**
     * 取消退货单
     */
    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        returnService.cancel(id);
        return Result.success();
    }

    /**
     * 按订单ID取消退货单
     */
    @PostMapping("/cancel-by-order/{orderId}")
    public Result<Void> cancelByOrderId(@PathVariable Long orderId) {
        returnService.cancelByOrderId(orderId);
        return Result.success();
    }

    /**
     * 文档导入退货单
     */
    @PostMapping("/import-file")
    public Result<Object> importFromFile(@RequestParam("file") MultipartFile file) {
        try {
            List<ReturnImportItem> importItems = parseReturnFile(file);
            int successCount = 0;
            List<String> errors = new ArrayList<>();

            for (int i = 0; i < importItems.size(); i++) {
                ReturnImportItem item = importItems.get(i);
                try {
                    // 通过平台单号查找订单
                    com.yiweilai.wms.order.entity.SalesOrder order =
                            orderMapper.findByPlatformOrderNo(item.platformOrderNo);
                    if (order == null) {
                        errors.add("平台单号 " + item.platformOrderNo + ": 订单不存在");
                        continue;
                    }

                    // 构建退货DTO
                    ReturnCreateDTO dto = new ReturnCreateDTO();
                    dto.setOrderId(order.getId());
                    dto.setReason(item.reason);
                    dto.setTrackingNo(item.trackingNo);
                    dto.setRemark(item.remark);

                    // 计算快递费
                    BigDecimal shippingFee = item.shippingFee;
                    if (shippingFee == null && item.estimatedWeight != null) {
                        ExpressFeeTemplate template = null;

                        // 优先使用指定的费用模板
                        if (item.feeTemplate != null && !item.feeTemplate.isEmpty()) {
                            // 根据模板名称查找（在"退货"公司下查找）
                            com.yiweilai.wms.express.entity.ExpressCompany returnCompany =
                                    expressCompanyMapper.findByName("退货");
                            if (returnCompany != null) {
                                List<ExpressFeeTemplate> templates = feeTemplateMapper.findByCompanyId(returnCompany.getId());
                                template = templates.stream()
                                        .filter(t -> item.feeTemplate.equals(t.getName()))
                                        .findFirst().orElse(null);
                            }
                        }

                        // 如果没有指定模板，使用"退货"公司的默认模板
                        if (template == null) {
                            com.yiweilai.wms.express.entity.ExpressCompany returnCompany =
                                    expressCompanyMapper.findByName("退货");
                            if (returnCompany != null) {
                                template = feeTemplateMapper.findByCompanyId(returnCompany.getId())
                                        .stream().filter(t -> t.getIsDefault() == 1).findFirst().orElse(null);
                            }
                        }

                        if (template != null) {
                            shippingFee = calculateFee(template, item.estimatedWeight);
                        }
                    }
                    dto.setShippingFee(shippingFee);

                    // 查询订单明细作为退货明细
                    List<com.yiweilai.wms.order.entity.SalesOrderItem> orderItems =
                            orderItemMapper.findByOrderId(order.getId());
                    List<ReturnCreateDTO.ReturnItemDTO> returnItems = new ArrayList<>();
                    for (com.yiweilai.wms.order.entity.SalesOrderItem orderItem : orderItems) {
                        ReturnCreateDTO.ReturnItemDTO returnItem = new ReturnCreateDTO.ReturnItemDTO();
                        returnItem.setSkuId(orderItem.getSkuId());
                        returnItem.setQuantity(orderItem.getQuantity());
                        returnItems.add(returnItem);
                    }
                    dto.setItems(returnItems);

                    returnService.create(dto);
                    successCount++;
                } catch (Exception e) {
                    errors.add("平台单号 " + item.platformOrderNo + ": " + e.getMessage());
                }
            }

            if (errors.isEmpty()) {
                return Result.success(successCount);
            } else {
                Map<String, Object> result = new HashMap<>();
                result.put("successCount", successCount);
                result.put("totalCount", importItems.size());
                result.put("errors", errors);
                return Result.success(result);
            }
        } catch (Exception e) {
            log.error("文件解析失败", e);
            return Result.error(400, "文件解析失败: " + e.getMessage());
        }
    }

    private List<ReturnImportItem> parseReturnFile(MultipartFile file) throws Exception {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("文件名为空");
        }

        List<ReturnImportItem> items = new ArrayList<>();

        if (filename.endsWith(".csv")) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
                String headerLine = reader.readLine();
                if (headerLine == null) {
                    throw new IllegalArgumentException("文件为空");
                }

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;
                    String[] parts = line.split(",", -1);
                    if (parts.length < 2) continue;

                    ReturnImportItem item = new ReturnImportItem();
                    item.platformOrderNo = parts[0].trim();
                    item.reason = parts[1].trim();
                    item.trackingNo = parts.length > 2 ? parts[2].trim() : null;
                    item.feeTemplate = parts.length > 3 ? parts[3].trim() : null;
                    item.estimatedWeight = parts.length > 4 && !parts[4].trim().isEmpty() ?
                            new BigDecimal(parts[4].trim()) : null;
                    item.shippingFee = parts.length > 5 && !parts[5].trim().isEmpty() ?
                            new BigDecimal(parts[5].trim()) : null;
                    item.remark = parts.length > 6 ? parts[6].trim() : null;
                    items.add(item);
                }
            }
        } else if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
            try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
                Sheet sheet = workbook.getSheetAt(0);
                if (sheet == null || sheet.getPhysicalNumberOfRows() <= 1) {
                    throw new IllegalArgumentException("文件为空或只有表头");
                }

                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;

                    String platformOrderNo = getCellValue(row, 0);
                    if (platformOrderNo.isEmpty()) continue;

                    ReturnImportItem item = new ReturnImportItem();
                    item.platformOrderNo = platformOrderNo;
                    item.reason = getCellValue(row, 1);
                    item.trackingNo = getCellValueOrNull(row, 2);
                    item.feeTemplate = getCellValueOrNull(row, 3);
                    item.estimatedWeight = parseBigDecimal(getCellValueOrNull(row, 4));
                    item.shippingFee = parseBigDecimal(getCellValueOrNull(row, 5));
                    item.remark = getCellValueOrNull(row, 6);
                    items.add(item);
                }
            }
        } else {
            throw new IllegalArgumentException("不支持的文件格式");
        }

        return items;
    }

    private String getCellValue(Row row, int cellIndex) {
        Cell cell = row.getCell(cellIndex);
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue().trim();
            case NUMERIC:
                double numValue = cell.getNumericCellValue();
                if (numValue == Math.floor(numValue) && !Double.isInfinite(numValue)) {
                    return String.valueOf((long) numValue);
                }
                return String.valueOf(numValue);
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            default: return "";
        }
    }

    private String getCellValueOrNull(Row row, int cellIndex) {
        String value = getCellValue(row, cellIndex);
        return value.isEmpty() ? null : value;
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isEmpty()) return null;
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal calculateFee(ExpressFeeTemplate template, BigDecimal weight) {
        if (template.getFirstWeight() != null && template.getFirstFee() != null) {
            // 首重续重
            if (weight.compareTo(template.getFirstWeight()) <= 0) {
                return template.getFirstFee();
            }
            if (template.getAdditionalWeight() != null && template.getAdditionalFee() != null) {
                BigDecimal extraWeight = weight.subtract(template.getFirstWeight());
                int extraUnits = extraWeight.divide(template.getAdditionalWeight(), 0, java.math.RoundingMode.UP).intValue();
                return template.getFirstFee().add(template.getAdditionalFee().multiply(new BigDecimal(extraUnits)));
            }
        }
        return null;
    }

    private static class ReturnImportItem {
        String platformOrderNo;
        String reason;
        String trackingNo;
        String feeTemplate;
        BigDecimal estimatedWeight;
        BigDecimal shippingFee;
        String remark;
    }
}
