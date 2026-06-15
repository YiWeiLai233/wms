package com.yiweilai.wms.alert.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.yiweilai.wms.alert.dto.StockAlertTemplateCreateDTO;
import com.yiweilai.wms.alert.dto.StockAlertTemplateUpdateDTO;
import com.yiweilai.wms.alert.entity.StockAlertConfig;
import com.yiweilai.wms.alert.entity.StockAlertTemplate;
import com.yiweilai.wms.alert.mapper.StockAlertConfigMapper;
import com.yiweilai.wms.alert.mapper.StockAlertTemplateMapper;
import com.yiweilai.wms.alert.service.StockAlertTemplateService;
import com.yiweilai.wms.alert.vo.StockAlertTemplateVO;
import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.exception.BusinessException;
import com.yiweilai.wms.exception.ErrorCode;
import com.yiweilai.wms.product.entity.ProductSku;
import com.yiweilai.wms.product.mapper.ProductMapper;
import com.yiweilai.wms.product.mapper.ProductSkuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 预警模板 Service 实现
 */
@Service
@RequiredArgsConstructor
public class StockAlertTemplateServiceImpl implements StockAlertTemplateService {

    private final StockAlertTemplateMapper templateMapper;
    private final StockAlertConfigMapper alertConfigMapper;
    private final ProductMapper productMapper;
    private final ProductSkuMapper skuMapper;

    @Override
    public PageResult<StockAlertTemplateVO> findByPage(String keyword, Integer enabled, Integer page, Integer size) {
        PageHelper.startPage(page, size);
        List<StockAlertTemplateVO> list = templateMapper.findByPage(keyword, enabled);
        PageInfo<StockAlertTemplateVO> pageInfo = new PageInfo<>(list);

        PageResult<StockAlertTemplateVO> result = new PageResult<>();
        result.setTotal(pageInfo.getTotal());
        result.setList(list);
        return result;
    }

    @Override
    public StockAlertTemplateVO getById(Long id) {
        StockAlertTemplate template = templateMapper.findById(id);
        if (template == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "预警模板不存在");
        }

        StockAlertTemplateVO vo = new StockAlertTemplateVO();
        BeanUtils.copyProperties(template, vo);
        vo.setProductCount(templateMapper.countProductsByTemplateId(id));
        return vo;
    }

    @Override
    public List<StockAlertTemplateVO> findEnabledList() {
        return templateMapper.findEnabledList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(StockAlertTemplateCreateDTO dto) {
        // 校验：缺货阈值不能大于低库存阈值
        if (dto.getOutOfStockThreshold() > dto.getLowStockThreshold()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "缺货阈值不能大于低库存阈值");
        }

        StockAlertTemplate template = new StockAlertTemplate();
        BeanUtils.copyProperties(dto, template);
        if (template.getEnabled() == null) {
            template.setEnabled(1);
        }
        templateMapper.insert(template);
        return template.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(StockAlertTemplateUpdateDTO dto) {
        StockAlertTemplate template = templateMapper.findById(dto.getId());
        if (template == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "预警模板不存在");
        }

        // 校验：缺货阈值不能大于低库存阈值
        Integer lowThreshold = dto.getLowStockThreshold() != null ? dto.getLowStockThreshold() : template.getLowStockThreshold();
        Integer outThreshold = dto.getOutOfStockThreshold() != null ? dto.getOutOfStockThreshold() : template.getOutOfStockThreshold();
        if (outThreshold > lowThreshold) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "缺货阈值不能大于低库存阈值");
        }

        // 更新模板
        BeanUtils.copyProperties(dto, template);
        templateMapper.update(template);

        // 自动同步：更新所有关联该模板的SKU的预警配置
        syncAlertConfigToRelatedSkus(dto.getId(), lowThreshold, outThreshold);
    }

    /**
     * 同步模板阈值到关联的SKU预警配置
     */
    private void syncAlertConfigToRelatedSkus(Long templateId, Integer lowThreshold, Integer outThreshold) {
        // 查找所有关联该模板的商品的SKU
        List<Long> productIds = productMapper.findProductIdsByTemplateId(templateId);
        if (productIds == null || productIds.isEmpty()) {
            return;
        }

        for (Long productId : productIds) {
            List<ProductSku> skuList = skuMapper.findByProductId(productId);
            if (skuList == null) {
                continue;
            }

            for (ProductSku sku : skuList) {
                // 查找该SKU的通用预警配置（warehouse_id IS NULL）
                StockAlertConfig existingConfig = alertConfigMapper.findBySkuId(sku.getId());

                if (existingConfig != null) {
                    // 更新现有配置
                    existingConfig.setLowStockThreshold(lowThreshold);
                    existingConfig.setOutOfStockThreshold(outThreshold);
                    alertConfigMapper.update(existingConfig);
                } else {
                    // 创建新配置
                    StockAlertConfig newConfig = new StockAlertConfig();
                    newConfig.setSkuId(sku.getId());
                    newConfig.setLowStockThreshold(lowThreshold);
                    newConfig.setOutOfStockThreshold(outThreshold);
                    newConfig.setEnabled(1);
                    alertConfigMapper.insert(newConfig);
                }
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        StockAlertTemplate template = templateMapper.findById(id);
        if (template == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "预警模板不存在");
        }

        // 检查是否有关联商品
        int productCount = templateMapper.countProductsByTemplateId(id);
        if (productCount > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该模板已关联" + productCount + "个商品，无法删除");
        }

        templateMapper.deleteById(id);
    }
}
