package com.yiweilai.wms.express.mapper;

import com.yiweilai.wms.express.entity.ExpressFeeStep;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 快递费用阶梯 Mapper
 */
@Mapper
public interface ExpressFeeStepMapper {

    /**
     * 根据模板ID查询阶梯列表
     */
    List<ExpressFeeStep> findByTemplateId(@Param("templateId") Long templateId);

    /**
     * 根据模板ID和重量查询匹配的阶梯
     */
    ExpressFeeStep findByTemplateIdAndWeight(@Param("templateId") Long templateId,
                                              @Param("weight") BigDecimal weight);

    /**
     * 新增
     */
    int insert(ExpressFeeStep step);

    /**
     * 根据模板ID删除所有阶梯
     */
    int deleteByTemplateId(@Param("templateId") Long templateId);
}
