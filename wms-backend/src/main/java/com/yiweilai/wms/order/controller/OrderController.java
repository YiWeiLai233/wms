package com.yiweilai.wms.order.controller;

import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.common.Result;
import com.yiweilai.wms.order.dto.OrderImportDTO;
import com.yiweilai.wms.order.dto.OrderQueryDTO;
import com.yiweilai.wms.order.dto.OrderStatusUpdateDTO;
import com.yiweilai.wms.order.service.OrderService;
import com.yiweilai.wms.order.vo.OrderVO;
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
import java.util.List;

/**
 * 订单 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 订单列表（分页）
     */
    @GetMapping
    public Result<PageResult<OrderVO>> list(OrderQueryDTO query) {
        return Result.success(orderService.findByPage(query));
    }

    /**
     * 根据ID查询订单详情
     */
    @GetMapping("/{id}")
    public Result<OrderVO> getById(@PathVariable Long id) {
        return Result.success(orderService.getById(id));
    }

    /**
     * 导入订单
     */
    @PostMapping("/import")
    public Result<Long> importOrder(@Valid @RequestBody OrderImportDTO dto) {
        return Result.success(orderService.importOrder(dto));
    }

    /**
     * 文档导入订单
     */
    @PostMapping("/import-file")
    public Result<Integer> importFromFile(@RequestParam("file") MultipartFile file,
                                          @RequestParam("warehouseId") Long warehouseId) {
        try {
            List<OrderImportDTO> orders = parseFile(file, warehouseId);
            int count = 0;
            for (OrderImportDTO dto : orders) {
                try {
                    orderService.importOrder(dto);
                    count++;
                } catch (Exception e) {
                    log.warn("导入订单失败: {}", e.getMessage());
                }
            }
            return Result.success(count);
        } catch (Exception e) {
            log.error("文件解析失败", e);
            return Result.error(400, "文件解析失败: " + e.getMessage());
        }
    }

    /**
     * 解析文件
     */
    private List<OrderImportDTO> parseFile(MultipartFile file, Long warehouseId) throws Exception {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("文件名为空");
        }

        if (filename.endsWith(".csv")) {
            return parseCsv(file, warehouseId);
        } else if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
            return parseExcel(file, warehouseId);
        } else {
            throw new IllegalArgumentException("不支持的文件格式");
        }
    }

    /**
     * 解析 CSV 文件
     */
    private List<OrderImportDTO> parseCsv(MultipartFile file, Long warehouseId) throws Exception {
        List<OrderImportDTO> orders = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine(); // 跳过表头
            if (headerLine == null) {
                throw new IllegalArgumentException("文件为空");
            }
            boolean legacyTemplate = headerLine.contains("expressCompany") || headerLine.contains("warehouseId");

            String line;
            OrderImportDTO currentOrder = null;
            String lastOrderNo = "";

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                String[] parts = line.split(",");
                if (parts.length < 8) {
                    continue;
                }

                String orderNo = parts[0].trim();
                String receiverName = legacyTemplate ? parts[2].trim() : parts[1].trim();
                String receiverPhone = legacyTemplate ? parts[3].trim() : parts[2].trim();
                String receiverAddress = legacyTemplate ? parts[4].trim() : parts[3].trim();
                String remark = legacyTemplate
                        ? (parts.length > 6 ? parts[6].trim() : "")
                        : (parts.length > 4 ? parts[4].trim() : "");
                String skuCode = legacyTemplate
                        ? (parts.length > 7 ? parts[7].trim() : "")
                        : (parts.length > 5 ? parts[5].trim() : "");
                int quantity = legacyTemplate
                        ? (parts.length > 9 ? Integer.parseInt(parts[9].trim()) : 1)
                        : (parts.length > 6 ? Integer.parseInt(parts[6].trim()) : 1);
                BigDecimal unitPrice = legacyTemplate
                        ? (parts.length > 10 ? new BigDecimal(parts[10].trim()) : BigDecimal.ZERO)
                        : (parts.length > 7 ? new BigDecimal(parts[7].trim()) : BigDecimal.ZERO);

                // 如果是新订单号，创建新的订单
                if (!orderNo.equals(lastOrderNo)) {
                    currentOrder = new OrderImportDTO();
                    currentOrder.setPlatformOrderNo(orderNo);
                    currentOrder.setWarehouseId(warehouseId);
                    currentOrder.setReceiverName(receiverName);
                    currentOrder.setReceiverPhone(receiverPhone);
                    currentOrder.setReceiverAddress(receiverAddress);
                    currentOrder.setRemark(remark);
                    currentOrder.setItems(new ArrayList<>());
                    orders.add(currentOrder);
                    lastOrderNo = orderNo;
                }

                // 添加订单明细
                if (currentOrder != null) {
                    OrderImportDTO.OrderItemDTO item = new OrderImportDTO.OrderItemDTO();
                    item.setSkuCode(skuCode);
                    item.setQuantity(quantity);
                    item.setUnitPrice(unitPrice);
                    currentOrder.getItems().add(item);
                }
            }
        }
        return orders;
    }

    /**
     * 解析 Excel 文件
     */
    private List<OrderImportDTO> parseExcel(MultipartFile file, Long warehouseId) throws Exception {
        List<OrderImportDTO> orders = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() <= 1) {
                throw new IllegalArgumentException("文件为空或只有表头");
            }

            OrderImportDTO currentOrder = null;
            String lastOrderNo = "";
            Row headerRow = sheet.getRow(0);
            String headerText = headerRow == null ? "" : getCellValue(headerRow, 0) + "," + getCellValue(headerRow, 1) + "," + getCellValue(headerRow, 5);
            boolean legacyTemplate = headerText.contains("warehouseId") || headerText.contains("expressCompany");

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String orderNo = getCellValue(row, 0);
                if (orderNo.isEmpty()) continue;

                String receiverName = legacyTemplate ? getCellValue(row, 2) : getCellValue(row, 1);
                String receiverPhone = legacyTemplate ? getCellValue(row, 3) : getCellValue(row, 2);
                String receiverAddress = legacyTemplate ? getCellValue(row, 4) : getCellValue(row, 3);
                String remark = legacyTemplate ? getCellValue(row, 6) : getCellValue(row, 4);
                String skuCode = legacyTemplate ? getCellValue(row, 7) : getCellValue(row, 5);
                String quantityStr = legacyTemplate ? getCellValue(row, 9) : getCellValue(row, 6);
                String priceStr = legacyTemplate ? getCellValue(row, 10) : getCellValue(row, 7);

                int quantity = quantityStr.isEmpty() ? 1 : Integer.parseInt(quantityStr);
                BigDecimal unitPrice = priceStr.isEmpty() ? BigDecimal.ZERO : new BigDecimal(priceStr);

                // 如果是新订单号，创建新的订单
                if (!orderNo.equals(lastOrderNo)) {
                    currentOrder = new OrderImportDTO();
                    currentOrder.setPlatformOrderNo(orderNo);
                    currentOrder.setWarehouseId(warehouseId);
                    currentOrder.setReceiverName(receiverName);
                    currentOrder.setReceiverPhone(receiverPhone);
                    currentOrder.setReceiverAddress(receiverAddress);
                    currentOrder.setRemark(remark);
                    currentOrder.setItems(new ArrayList<>());
                    orders.add(currentOrder);
                    lastOrderNo = orderNo;
                }

                // 添加订单明细
                if (currentOrder != null) {
                    OrderImportDTO.OrderItemDTO item = new OrderImportDTO.OrderItemDTO();
                    item.setSkuCode(skuCode);
                    item.setQuantity(quantity);
                    item.setUnitPrice(unitPrice);
                    currentOrder.getItems().add(item);
                }
            }
        }
        return orders;
    }

    /**
     * 获取单元格值
     */
    private String getCellValue(Row row, int cellIndex) {
        Cell cell = row.getCell(cellIndex);
        if (cell == null) return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                // 处理数字，避免科学计数法
                double numValue = cell.getNumericCellValue();
                if (numValue == Math.floor(numValue) && !Double.isInfinite(numValue)) {
                    return String.valueOf((long) numValue);
                }
                return String.valueOf(numValue);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    /**
     * 更新订单状态
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @Valid @RequestBody OrderStatusUpdateDTO dto) {
        dto.setOrderId(id);
        orderService.updateStatus(dto);
        return Result.success();
    }
}
