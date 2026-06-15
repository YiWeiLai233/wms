package com.yiweilai.wms.product.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.yiweilai.wms.alert.entity.StockAlertConfig;
import com.yiweilai.wms.alert.entity.StockAlertTemplate;
import com.yiweilai.wms.alert.mapper.StockAlertConfigMapper;
import com.yiweilai.wms.alert.mapper.StockAlertTemplateMapper;
import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.exception.BusinessException;
import com.yiweilai.wms.exception.ErrorCode;
import com.yiweilai.wms.product.dto.ProductQueryDTO;
import com.yiweilai.wms.product.dto.ProductSaveDTO;
import com.yiweilai.wms.product.entity.Product;
import com.yiweilai.wms.product.entity.ProductSku;
import com.yiweilai.wms.product.mapper.ProductMapper;
import com.yiweilai.wms.product.mapper.ProductSkuMapper;
import com.yiweilai.wms.product.service.ProductService;
import com.yiweilai.wms.product.vo.ProductSkuVO;
import com.yiweilai.wms.product.vo.ProductVO;
import com.yiweilai.wms.stock.entity.Stock;
import com.yiweilai.wms.stock.entity.StockLog;
import com.yiweilai.wms.stock.mapper.StockLogMapper;
import com.yiweilai.wms.stock.mapper.StockMapper;
import com.yiweilai.wms.warehouse.entity.Warehouse;
import com.yiweilai.wms.warehouse.entity.WarehouseShelf;
import com.yiweilai.wms.warehouse.mapper.WarehouseMapper;
import com.yiweilai.wms.warehouse.mapper.WarehouseShelfMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 商品 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;
    private final ProductSkuMapper skuMapper;
    private final WarehouseMapper warehouseMapper;
    private final WarehouseShelfMapper shelfMapper;
    private final StockMapper stockMapper;
    private final StockLogMapper stockLogMapper;
    private final StockAlertTemplateMapper alertTemplateMapper;
    private final StockAlertConfigMapper alertConfigMapper;

    @Override
    public PageResult<ProductVO> findByPage(ProductQueryDTO query) {
        PageHelper.startPage(query.getPage(), query.getSize());
        List<Product> products = productMapper.findByPage(
                query.getKeyword(), query.getShelfId(), query.getStatus());

        PageInfo<Product> pageInfo = new PageInfo<>(products);

        List<ProductVO> voList = products.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        PageResult<ProductVO> result = new PageResult<>();
        result.setTotal(pageInfo.getTotal());
        result.setList(voList);
        return result;
    }

    @Override
    public ProductVO getById(Long id) {
        Product product = productMapper.findById(id);
        if (product == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "商品不存在");
        }

        ProductVO vo = convertToVO(product);

        // 查询关联的SKU
        List<ProductSkuVO> skuList = skuMapper.findByProductId(id).stream()
                .map(sku -> convertSkuToVO(sku, product))
                .collect(Collectors.toList());
        vo.setSkuList(skuList);

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ProductSaveDTO dto) {
        // 检查SPU编码唯一性
        Product existing = productMapper.findBySpuCode(dto.getSpuCode());
        if (existing != null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "SPU编码已存在");
        }

        Product product = new Product();
        BeanUtils.copyProperties(dto, product);
        productMapper.insert(product);

        createSizeSkus(dto, product);

        return product.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(ProductSaveDTO dto) {
        Product product = productMapper.findById(dto.getId());
        if (product == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "商品不存在");
        }

        // 检查SPU编码唯一性（排除自身）
        if (!product.getSpuCode().equals(dto.getSpuCode())) {
            Product existing = productMapper.findBySpuCode(dto.getSpuCode());
            if (existing != null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "SPU编码已存在");
            }
        }

        // 检查预警模板是否发生变化
        Long oldTemplateId = product.getAlertTemplateId();
        Long newTemplateId = dto.getAlertTemplateId();

        BeanUtils.copyProperties(dto, product);
        productMapper.update(product);

        // 如果预警模板发生变化，应用新模板到所有关联的SKU
        if (newTemplateId != null && !newTemplateId.equals(oldTemplateId)) {
            applyAlertTemplateToAllSkus(product.getId(), newTemplateId);
        }
    }

    /**
     * 应用预警模板到商品的所有SKU
     */
    private void applyAlertTemplateToAllSkus(Long productId, Long templateId) {
        StockAlertTemplate template = alertTemplateMapper.findById(templateId);
        if (template == null || template.getEnabled() != 1) {
            return;
        }

        List<ProductSku> skuList = skuMapper.findByProductId(productId);
        if (skuList == null || skuList.isEmpty()) {
            return;
        }

        for (ProductSku sku : skuList) {
            applyAlertTemplate(templateId, sku.getId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Product product = productMapper.findById(id);
        if (product == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "商品不存在");
        }
        productMapper.deleteById(id);
    }

    private ProductVO convertToVO(Product product) {
        ProductVO vo = new ProductVO();
        BeanUtils.copyProperties(product, vo);

        // 查询仓库信息
        if (product.getWarehouseId() != null) {
            Warehouse warehouse = warehouseMapper.findById(product.getWarehouseId());
            if (warehouse != null) {
                vo.setWarehouseName(warehouse.getName());
            }
        }

        // 查询货架信息
        if (product.getShelfId() != null) {
            WarehouseShelf shelf = shelfMapper.findById(product.getShelfId());
            if (shelf != null) {
                vo.setShelfCode(shelf.getCode());
                vo.setCategoryName(shelf.getCategoryName());
            }
        }

        // 查询预警模板信息
        if (product.getAlertTemplateId() != null) {
            StockAlertTemplate template = alertTemplateMapper.findById(product.getAlertTemplateId());
            if (template != null) {
                vo.setAlertTemplateName(template.getName());
            }
        }

        return vo;
    }

    private void createSizeSkus(ProductSaveDTO dto, Product product) {
        if (dto.getSkuList() == null || dto.getSkuList().isEmpty()) {
            return;
        }

        WarehouseShelf inboundShelf = null;
        boolean needsInitialStock = dto.getSkuList().stream()
                .anyMatch(item -> normalizeQuantity(item.getQuantity()) > 0);
        if (needsInitialStock) {
            inboundShelf = shelfMapper.findById(dto.getShelfId());
            if (inboundShelf == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "商品货架不存在，无法生成初始库存");
            }
        }

        for (ProductSaveDTO.SizeSkuDTO item : dto.getSkuList()) {
            String sizeValue = trimToNull(item.getSizeValue());
            if (sizeValue == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "码数不能为空");
            }

            String skuCode = trimToNull(item.getSkuCode());
            if (skuCode == null) {
                skuCode = dto.getSpuCode() + "-" + sizeValue;
            }
            if (skuMapper.findBySkuCode(skuCode) != null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "SKU编码已存在: " + skuCode);
            }

            ProductSku sku = new ProductSku();
            sku.setProductId(product.getId());
            sku.setSkuCode(skuCode);
            sku.setName(defaultText(item.getName(), product.getName() + "-" + sizeValue));
            sku.setSizeValue(sizeValue);
            sku.setQuantity(0);
            sku.setCostPrice(defaultDecimal(item.getCostPrice()));
            sku.setSalePrice(item.getSalePrice() != null ? item.getSalePrice() : defaultDecimal(dto.getPrice()));
            sku.setWeight(item.getWeight());
            sku.setVolume(item.getVolume());
            sku.setImage(item.getImage());
            sku.setStatus(product.getStatus() == null ? 1 : product.getStatus());
            skuMapper.insert(sku);

            // 如果商品关联了预警模板，自动创建预警配置
            applyAlertTemplate(product.getAlertTemplateId(), sku.getId());

            int quantity = normalizeQuantity(item.getQuantity());
            if (quantity > 0) {
                inboundInitialStock(sku.getId(), inboundShelf, quantity);
            }
        }
    }

    /**
     * 应用预警模板到SKU
     */
    private void applyAlertTemplate(Long templateId, Long skuId) {
        if (templateId == null) {
            return;
        }

        StockAlertTemplate template = alertTemplateMapper.findById(templateId);
        if (template == null || template.getEnabled() != 1) {
            return;
        }

        // 检查是否已有预警配置
        StockAlertConfig existingConfig = alertConfigMapper.findBySkuId(skuId);
        if (existingConfig != null) {
            // 更新现有配置
            existingConfig.setLowStockThreshold(template.getLowStockThreshold());
            existingConfig.setOutOfStockThreshold(template.getOutOfStockThreshold());
            alertConfigMapper.update(existingConfig);
        } else {
            // 创建新配置
            StockAlertConfig config = new StockAlertConfig();
            config.setSkuId(skuId);
            config.setLowStockThreshold(template.getLowStockThreshold());
            config.setOutOfStockThreshold(template.getOutOfStockThreshold());
            config.setEnabled(1);
            alertConfigMapper.insert(config);
        }
    }

    private void inboundInitialStock(Long skuId, WarehouseShelf shelf, int quantity) {
        Stock stock = new Stock();
        stock.setSkuId(skuId);
        stock.setWarehouseId(shelf.getWarehouseId());
        stock.setQuantity(quantity);
        stock.setLockedQty(0);
//        stock.setDefectiveQty(0);
        stockMapper.insert(stock);

        StockLog stockLog = new StockLog();
        stockLog.setBizType("INBOUND");
        stockLog.setBizNo("SKU_" + skuId);
        stockLog.setSkuId(skuId);
        stockLog.setWarehouseId(shelf.getWarehouseId());
        stockLog.setQuantityBefore(0);
        stockLog.setQuantityChange(quantity);
        stockLog.setQuantityAfter(quantity);
        stockLog.setRemark("商品创建初始入库");
        stockLogMapper.insert(stockLog);
    }

    private ProductSkuVO convertSkuToVO(ProductSku sku, Product product) {
        ProductSkuVO skuVO = new ProductSkuVO();
        BeanUtils.copyProperties(sku, skuVO);
        skuVO.setProductName(product.getName());
        skuVO.setAvailableQty(defaultZero(sku.getAvailableQty()));
        skuVO.setLockedQty(defaultZero(sku.getLockedQty()));
//        skuVO.setDefectiveQty(defaultZero(sku.getDefectiveQty()));
        skuVO.setTotalQty(defaultZero(sku.getTotalQty()));
        skuVO.setQuantity(skuVO.getAvailableQty());
        return skuVO;
    }

    private int normalizeQuantity(Integer quantity) {
        if (quantity == null) {
            return 0;
        }
        if (quantity < 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "码数初始数量不能小于0");
        }
        return quantity;
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String defaultText(String value, String fallback) {
        String normalized = trimToNull(value);
        return normalized == null ? fallback : normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private int defaultZero(Integer value) {
        return value == null ? 0 : value;
    }
}
