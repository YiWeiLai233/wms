package com.yiweilai.wms.alert.mapper;

import com.yiweilai.wms.alert.entity.StockAlertTemplate;
import com.yiweilai.wms.alert.vo.StockAlertTemplateVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 预警模板 Mapper
 */
@Mapper
public interface StockAlertTemplateMapper {

    /**
     * 分页查询模板列表
     */
    List<StockAlertTemplateVO> findByPage(@Param("keyword") String keyword,
                                          @Param("enabled") Integer enabled);

    /**
     * 根据ID查询
     */
    StockAlertTemplate findById(@Param("id") Long id);

    /**
     * 查询启用的模板列表（用于下拉选择）
     */
    List<StockAlertTemplateVO> findEnabledList();

    /**
     * 新增模板
     */
    int insert(StockAlertTemplate template);

    /**
     * 更新模板
     */
    int update(StockAlertTemplate template);

    /**
     * 逻辑删除
     */
    int deleteById(@Param("id") Long id);

    /**
     * 查询关联该模板的商品数量
     */
    int countProductsByTemplateId(@Param("templateId") Long templateId);
}
