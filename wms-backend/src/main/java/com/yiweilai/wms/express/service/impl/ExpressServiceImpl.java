package com.yiweilai.wms.express.service.impl;

import com.yiweilai.wms.express.dto.ExpressQueryDTO;
import com.yiweilai.wms.express.entity.ExpressFeeStep;
import com.yiweilai.wms.express.entity.ExpressFeeTemplate;
import com.yiweilai.wms.express.mapper.ExpressFeeStepMapper;
import com.yiweilai.wms.express.mapper.ExpressFeeTemplateMapper;
import com.yiweilai.wms.express.service.ExpressService;
import com.yiweilai.wms.express.vo.ExpressInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 快递 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpressServiceImpl implements ExpressService {

    @Value("${express.kuaidi100.enabled:false}")
    private boolean kuaidi100Enabled;

    @Value("${express.kuaidi100.key:}")
    private String kuaidi100Key;

    @Value("${express.kuaidi100.customer:}")
    private String kuaidi100Customer;

    private final ExpressFeeTemplateMapper templateMapper;
    private final ExpressFeeStepMapper stepMapper;

    @Override
    public ExpressInfoVO query(String trackingNo, String carrier) {
        // 如果启用了快递100，尝试调用API
        if (kuaidi100Enabled && !kuaidi100Key.isEmpty()) {
            try {
                return queryFromKuaidi100(trackingNo, carrier);
            } catch (Exception e) {
                log.warn("快递100查询失败: {}", e.getMessage());
            }
        }

        // 返回空结果（前端会显示提示）
        return null;
    }

    @Override
    public ExpressInfoVO.FeeInfo calculateFee(ExpressQueryDTO dto) {
        ExpressInfoVO.FeeInfo feeInfo = new ExpressInfoVO.FeeInfo();

        BigDecimal totalWeight = dto.getTotalWeight();
        if (totalWeight == null || totalWeight.compareTo(BigDecimal.ZERO) == 0) {
            totalWeight = BigDecimal.ONE;
        }

        BigDecimal fee;
        String billingMethod;

        // 如果指定了模板ID，使用模板计算
        if (dto.getTemplateId() != null) {
            fee = calculateByTemplate(dto.getTemplateId(), totalWeight);
            billingMethod = "阶梯计费";
        } else {
            // 使用默认模板
            ExpressFeeTemplate defaultTemplate = templateMapper.findDefault();
            if (defaultTemplate != null) {
                fee = calculateByTemplate(defaultTemplate.getId(), totalWeight);
                billingMethod = "阶梯计费（默认）";
            } else {
                // 没有配置模板，使用默认费率
                fee = calculateDefaultFee(totalWeight);
                billingMethod = "默认费率";
            }
        }

        feeInfo.setTotalWeight(totalWeight);
        feeInfo.setFirstWeight(new BigDecimal("1"));
        feeInfo.setFirstWeightFee(fee);
        feeInfo.setAdditionalWeightFee(BigDecimal.ZERO);
        feeInfo.setTotalFee(fee.setScale(2, RoundingMode.HALF_UP));
        feeInfo.setBillingMethod(billingMethod);

        return feeInfo;
    }

    /**
     * 按模板阶梯计算费用
     */
    private BigDecimal calculateByTemplate(Long templateId, BigDecimal weight) {
        ExpressFeeStep step = stepMapper.findByTemplateIdAndWeight(templateId, weight);
        if (step != null) {
            return step.getFee();
        }

        // 如果没有匹配的阶梯，查找最大阶梯
        // 这里简化处理，返回0或可以抛出异常
        log.warn("未找到匹配的费用阶梯: templateId={}, weight={}", templateId, weight);
        return BigDecimal.ZERO;
    }

    /**
     * 默认费率计算（兼容没有配置模板的情况）
     */
    private BigDecimal calculateDefaultFee(BigDecimal weight) {
        BigDecimal firstWeightFee = new BigDecimal("12");
        BigDecimal additionalWeightFee = new BigDecimal("5");
        BigDecimal firstWeight = new BigDecimal("1");

        BigDecimal fee = firstWeightFee;
        if (weight.compareTo(firstWeight) > 0) {
            BigDecimal additionalWeight = weight.subtract(firstWeight);
            int additionalKg = additionalWeight.intValue();
            if (additionalWeight.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) > 0) {
                additionalKg++;
            }
            fee = fee.add(additionalWeightFee.multiply(new BigDecimal(additionalKg)));
        }
        return fee;
    }

    /**
     * 调用快递100 API查询
     */
    private ExpressInfoVO queryFromKuaidi100(String trackingNo, String carrier) {
        // TODO: 实现快递100 API调用
        log.info("快递100查询 - 单号: {}, 公司: {}", trackingNo, carrier);
        return null;
    }
}
