package com.yiweilai.wms.alert.service;

import com.yiweilai.wms.alert.dto.StockAlertTemplateCreateDTO;
import com.yiweilai.wms.alert.dto.StockAlertTemplateUpdateDTO;
import com.yiweilai.wms.alert.vo.StockAlertTemplateVO;
import com.yiweilai.wms.common.PageResult;

import java.util.List;

/**
 * 预警模板 Service 接口
 */
public interface StockAlertTemplateService {

    /**
     * 分页查询模板列表
     */
    PageResult<StockAlertTemplateVO> findByPage(String keyword, Integer enabled, Integer page, Integer size);

    /**
     * 根据ID查询模板详情
     */
    StockAlertTemplateVO getById(Long id);

    /**
     * 查询启用的模板列表（用于下拉选择）
     */
    List<StockAlertTemplateVO> findEnabledList();

    /**
     * 创建模板
     */
    Long create(StockAlertTemplateCreateDTO dto);

    /**
     * 更新模板（自动同步关联SKU的预警配置）
     */
    void update(StockAlertTemplateUpdateDTO dto);

    /**
     * 删除模板
     */
    void delete(Long id);
}
