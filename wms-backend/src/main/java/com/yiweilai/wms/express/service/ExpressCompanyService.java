package com.yiweilai.wms.express.service;

import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.express.dto.ExpressCompanySaveDTO;
import com.yiweilai.wms.express.vo.ExpressCompanyVO;

import java.util.List;

/**
 * 快递公司 Service
 */
public interface ExpressCompanyService {

    /**
     * 查询所有启用的公司（用于下拉选择）
     */
    List<ExpressCompanyVO> findAll();

    /**
     * 分页查询
     */
    PageResult<ExpressCompanyVO> findByPage(String name, Integer status, Integer page, Integer size);

    /**
     * 根据ID查询
     */
    ExpressCompanyVO getById(Long id);

    /**
     * 新增
     */
    Long create(ExpressCompanySaveDTO dto);

    /**
     * 修改
     */
    void update(ExpressCompanySaveDTO dto);

    /**
     * 删除
     */
    void delete(Long id);
}
