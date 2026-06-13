package com.yiweilai.wms.stock.service.impl;

import com.yiweilai.wms.exception.BusinessException;
import com.yiweilai.wms.exception.ErrorCode;
import com.yiweilai.wms.stock.dto.StockCheckCreateDTO;
import com.yiweilai.wms.stock.dto.StockCheckSubmitDTO;
import com.yiweilai.wms.stock.entity.Stock;
import com.yiweilai.wms.stock.entity.StockCheck;
import com.yiweilai.wms.stock.entity.StockCheckItem;
import com.yiweilai.wms.stock.entity.StockLog;
import com.yiweilai.wms.stock.mapper.StockCheckItemMapper;
import com.yiweilai.wms.stock.mapper.StockCheckMapper;
import com.yiweilai.wms.stock.mapper.StockLogMapper;
import com.yiweilai.wms.stock.mapper.StockMapper;
import com.yiweilai.wms.stock.service.StockCheckService;
import com.yiweilai.wms.stock.vo.StockCheckItemVO;
import com.yiweilai.wms.stock.vo.StockCheckVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 库存盘点 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockCheckServiceImpl implements StockCheckService {

    private final StockCheckMapper checkMapper;
    private final StockCheckItemMapper checkItemMapper;
    private final StockMapper stockMapper;
    private final StockLogMapper stockLogMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(StockCheckCreateDTO dto) {
        // 生成盘点单号
        String checkNo = "CHK" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());

        StockCheck check = new StockCheck();
        check.setCheckNo(checkNo);
        check.setWarehouseId(dto.getWarehouseId());
        check.setStatus(0); // 待盘点
        check.setRemark(dto.getRemark());
        checkMapper.insert(check);

        // 查询该仓库所有库存，生成盘点明细
        List<Stock> stocks = stockMapper.findByPage(null, null, null, null, dto.getWarehouseId(), null);
        for (Stock stock : stocks) {
            StockCheckItem item = new StockCheckItem();
            item.setCheckId(check.getId());
            item.setSkuId(stock.getSkuId());
            item.setSystemQty(stock.getQuantity());
            checkItemMapper.insert(item);
        }

        return check.getId();
    }

    @Override
    public StockCheckVO getById(Long id) {
        StockCheck check = checkMapper.findById(id);
        if (check == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "盘点单不存在");
        }

        StockCheckVO vo = new StockCheckVO();
        BeanUtils.copyProperties(check, vo);

        // 查询盘点明细
        List<StockCheckItemVO> items = checkItemMapper.findByCheckId(id).stream()
                .map(item -> {
                    StockCheckItemVO itemVO = new StockCheckItemVO();
                    BeanUtils.copyProperties(item, itemVO);
                    return itemVO;
                })
                .collect(Collectors.toList());
        vo.setItems(items);

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submit(StockCheckSubmitDTO dto) {
        StockCheck check = checkMapper.findById(dto.getCheckId());
        if (check == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "盘点单不存在");
        }
        if (check.getStatus() == 2) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "盘点单已完成");
        }

        // 更新盘点状态为盘点中
        if (check.getStatus() == 0) {
            checkMapper.updateStatus(dto.getCheckId(), 1);
        }

        // 处理每个盘点项
        for (StockCheckSubmitDTO.StockCheckItemDTO itemDTO : dto.getItems()) {
            StockCheckItem item = checkItemMapper.findById(itemDTO.getItemId());
            if (item == null) {
                continue;
            }

            int diffQty = itemDTO.getActualQty() - item.getSystemQty();
            checkItemMapper.updateActualQty(item.getId(), itemDTO.getActualQty(), diffQty);

            // 如果有差异，调整库存并写流水
            if (diffQty != 0) {
                // 查找该SKU在仓库的库存记录
                List<Stock> stocks = stockMapper.findAvailableBySkuAndWarehouse(item.getSkuId(), check.getWarehouseId());
                if (stocks != null && !stocks.isEmpty()) {
                    Stock stock = stocks.get(0);
                    int beforeQty = stock.getQuantity();
                    int afterQty = beforeQty + diffQty;

                    stockMapper.updateQuantity(stock.getId(), afterQty);

                    StockLog log = new StockLog();
                    log.setBizType("ADJUST");
                    log.setBizNo("CHK_" + check.getId());
                    log.setSkuId(item.getSkuId());
                    log.setWarehouseId(check.getWarehouseId());
                    log.setQuantityBefore(beforeQty);
                    log.setQuantityChange(diffQty);
                    log.setQuantityAfter(afterQty);
                    log.setRemark("盘点调整");
                    stockLogMapper.insert(log);
                }
            }
        }

        // 更新盘点状态为已完成
        checkMapper.updateStatus(dto.getCheckId(), 2);
    }
}
