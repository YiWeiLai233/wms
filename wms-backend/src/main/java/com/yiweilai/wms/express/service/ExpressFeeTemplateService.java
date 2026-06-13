package com.yiweilai.wms.express.service;

import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.express.dto.ExpressFeeTemplateSaveDTO;
import com.yiweilai.wms.express.vo.ExpressFeeTemplateVO;

import java.util.List;

/**
 * 快递费用模板 Service
 */
public interface ExpressFeeTemplateService {

    /**
     * 根据公司ID查询模板列表
     */
    List<ExpressFeeTemplateVO> findByCompanyId(Long companyId);

    /**
     * 分页查询
     */
    PageResult<ExpressFeeTemplateVO> findByPage(Long companyId, String name, Integer page, Integer size);

    /**
     * 根据ID查询详情（含阶梯）
     */
    ExpressFeeTemplateVO getById(Long id);

    /**
     * 查询默认模板
     */
    ExpressFeeTemplateVO getDefault();

    /**
     * 新增（含阶梯）
     */
    Long create(ExpressFeeTemplateSaveDTO dto);

    /**
     * 修改（含阶梯）
     */
    void update(ExpressFeeTemplateSaveDTO dto);

    /**
     * 删除
     */
    void delete(Long id);
}
