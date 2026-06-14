package com.yiweilai.wms.product.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.exception.BusinessException;
import com.yiweilai.wms.exception.ErrorCode;
import com.yiweilai.wms.product.dto.ProductBarcodeSaveDTO;
import com.yiweilai.wms.product.dto.ProductSkuQueryDTO;
import com.yiweilai.wms.product.dto.ProductSkuSaveDTO;
import com.yiweilai.wms.product.entity.Product;
import com.yiweilai.wms.product.entity.ProductBarcode;
import com.yiweilai.wms.product.entity.ProductSku;
import com.yiweilai.wms.product.mapper.ProductBarcodeMapper;
import com.yiweilai.wms.product.mapper.ProductMapper;
import com.yiweilai.wms.product.mapper.ProductSkuMapper;
import com.yiweilai.wms.stock.entity.Stock;
import com.yiweilai.wms.stock.entity.StockLog;
import com.yiweilai.wms.stock.mapper.StockLogMapper;
import com.yiweilai.wms.stock.mapper.StockMapper;
import com.yiweilai.wms.warehouse.entity.WarehouseShelf;
import com.yiweilai.wms.warehouse.mapper.WarehouseShelfMapper;
import com.yiweilai.wms.product.service.ProductSkuService;
import com.yiweilai.wms.product.vo.ProductBarcodeVO;
import com.yiweilai.wms.product.vo.ProductSkuListVO;
import com.yiweilai.wms.product.vo.ProductSkuVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商品SKU Service 实现
 */
@Service
@RequiredArgsConstructor
public class ProductSkuServiceImpl implements ProductSkuService {

    private final ProductSkuMapper skuMapper;
    private final ProductBarcodeMapper barcodeMapper;
    private final ProductMapper productMapper;
    private final WarehouseShelfMapper shelfMapper;
    private final StockMapper stockMapper;
    private final StockLogMapper stockLogMapper;

    @Override
    public List<ProductSkuVO> findByProductId(Long productId) {
        return skuMapper.findByProductId(productId).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public PageResult<ProductSkuListVO> findByPage(ProductSkuQueryDTO query) {
        PageHelper.startPage(query.getPage(), query.getSize());
        List<ProductSku> skuList = skuMapper.findByPage(query.getKeyword(), query.getStatus(), query.getWarehouseId());

        PageInfo<ProductSku> pageInfo = new PageInfo<>(skuList);

        // 批量查询商品信息
        List<Long> productIds = skuList.stream()
                .map(ProductSku::getProductId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, Product> productMap = productIds.stream()
                .map(id -> productMapper.findById(id))
                .filter(p -> p != null)
                .collect(Collectors.toMap(Product::getId, p -> p));

        // 批量查询货架信息
        List<Long> shelfIds = productMap.values().stream()
                .map(Product::getShelfId)
                .filter(s -> s != null)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, WarehouseShelf> shelfMap = shelfIds.stream()
                .map(id -> shelfMapper.findById(id))
                .filter(s -> s != null)
                .collect(Collectors.toMap(WarehouseShelf::getId, s -> s));

        List<ProductSkuListVO> voList = skuList.stream().map(sku -> {
            ProductSkuListVO vo = new ProductSkuListVO();
            BeanUtils.copyProperties(sku, vo);
            vo.setAvailableQty(defaultZero(sku.getAvailableQty()));
            vo.setLockedQty(defaultZero(sku.getLockedQty()));
            vo.setTotalQty(defaultZero(sku.getTotalQty()));
            vo.setQuantity(vo.getAvailableQty());

            Product product = productMap.get(sku.getProductId());
            if (product != null) {
                vo.setProductName(product.getName());
                if (product.getShelfId() != null) {
                    WarehouseShelf shelf = shelfMap.get(product.getShelfId());
                    if (shelf != null) {
                        vo.setShelfCode(shelf.getCode());
                        vo.setCategoryName(shelf.getCategoryName());
                    }
                }
            }
            return vo;
        }).collect(Collectors.toList());

        PageResult<ProductSkuListVO> result = new PageResult<>();
        result.setTotal(pageInfo.getTotal());
        result.setList(voList);
        return result;
    }

    @Override
    public ProductSkuVO getById(Long id) {
        ProductSku sku = skuMapper.findById(id);
        if (sku == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "SKU不存在");
        }
        return convertToVO(sku);
    }

    @Override
    public ProductSkuVO getBySkuCode(String skuCode) {
        ProductSku sku = skuMapper.findBySkuCode(skuCode);
        if (sku == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "SKU不存在");
        }
        return convertToVO(sku);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ProductSkuSaveDTO dto) {
        // 检查SKU编码唯一性
        ProductSku existing = skuMapper.findBySkuCode(dto.getSkuCode());
        if (existing != null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "SKU编码已存在");
        }

        Integer initialQuantity = dto.getInitialQuantity() != null ? dto.getInitialQuantity() : dto.getQuantity();
        if (initialQuantity == null) {
            initialQuantity = 0;
        }
        if (initialQuantity < 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Initial inbound quantity cannot be negative");
        }

        Product product = productMapper.findById(dto.getProductId());
        if (product == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Product not found");
        }
        if (initialQuantity > 0) {
            validateInboundLocation(dto);
        }

        ProductSku sku = new ProductSku();
        BeanUtils.copyProperties(dto, sku);
        sku.setQuantity(0);
        skuMapper.insert(sku);

        // 如果输入了数量，自动入库
        if (initialQuantity > 0) {
            inboundInitialStock(sku.getId(), dto, initialQuantity);
        }

        return sku.getId();
    }

    private void validateInboundLocation(ProductSkuSaveDTO dto) {
        if (dto.getWarehouseId() == null || dto.getShelfId() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Initial inbound requires warehouse and shelf");
        }
        WarehouseShelf shelf = shelfMapper.findById(dto.getShelfId());
        if (shelf == null || !dto.getWarehouseId().equals(shelf.getWarehouseId())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Shelf does not belong to selected warehouse");
        }
    }

    private void inboundInitialStock(Long skuId, ProductSkuSaveDTO dto, int quantity) {
        Stock stock = stockMapper.findBySkuAndWarehouse(skuId, dto.getWarehouseId());
        int beforeQty = stock == null || stock.getQuantity() == null ? 0 : stock.getQuantity();
        int afterQty = beforeQty + quantity;

        if (stock == null) {
            stock = new Stock();
            stock.setSkuId(skuId);
            stock.setWarehouseId(dto.getWarehouseId());
            stock.setQuantity(quantity);
            stock.setLockedQty(0);
//            stock.setDefectiveQty(0);
            stockMapper.insert(stock);
        } else {
            stockMapper.updateQuantity(stock.getId(), afterQty);
        }

        StockLog stockLog = new StockLog();
        stockLog.setBizType("INBOUND");
        stockLog.setBizNo("SKU_" + skuId);
        stockLog.setSkuId(skuId);
        stockLog.setWarehouseId(dto.getWarehouseId());
        stockLog.setQuantityBefore(beforeQty);
        stockLog.setQuantityChange(quantity);
        stockLog.setQuantityAfter(afterQty);
        stockLog.setRemark(dto.getInboundRemark() == null || dto.getInboundRemark().isBlank()
                ? "SKU initial inbound"
                : dto.getInboundRemark());
        stockLogMapper.insert(stockLog);
    }

    private int defaultZero(Integer value) {
        return value == null ? 0 : value;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(ProductSkuSaveDTO dto) {
        ProductSku sku = skuMapper.findById(dto.getId());
        if (sku == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "SKU不存在");
        }

        // 检查SKU编码唯一性（排除自身）
        if (!sku.getSkuCode().equals(dto.getSkuCode())) {
            ProductSku existing = skuMapper.findBySkuCode(dto.getSkuCode());
            if (existing != null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "SKU编码已存在");
            }
        }

        BeanUtils.copyProperties(dto, sku);
        skuMapper.update(sku);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ProductSku sku = skuMapper.findById(id);
        if (sku == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "SKU不存在");
        }
        skuMapper.deleteById(id);
    }

    private ProductSkuVO convertToVO(ProductSku sku) {
        ProductSkuVO vo = new ProductSkuVO();
        BeanUtils.copyProperties(sku, vo);
        vo.setAvailableQty(defaultZero(sku.getAvailableQty()));
        vo.setLockedQty(defaultZero(sku.getLockedQty()));
//        vo.setDefectiveQty(defaultZero(sku.getDefectiveQty()));
        vo.setTotalQty(defaultZero(sku.getTotalQty()));
        vo.setQuantity(vo.getAvailableQty());

        // 查询关联的条码
        List<ProductBarcodeVO> barcodeList = barcodeMapper.findBySkuId(sku.getId()).stream()
                .map(barcode -> {
                    ProductBarcodeVO barcodeVO = new ProductBarcodeVO();
                    BeanUtils.copyProperties(barcode, barcodeVO);
                    return barcodeVO;
                })
                .collect(Collectors.toList());
        vo.setBarcodeList(barcodeList);

        return vo;
    }
}
