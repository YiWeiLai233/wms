package com.yiweilai.wms.express.mapper;

import com.yiweilai.wms.express.entity.ExpressFeeTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 快递费用模板 Mapper
 */
@Mapper
public interface ExpressFeeTemplateMapper {

    /**
     * 根据公司ID查询模板列表
     */
    List<ExpressFeeTemplate> findByCompanyId(@Param("companyId") Long companyId);

    /**
     * 查询所有启用的模板
     */
    List<ExpressFeeTemplate> findAll();

    /**
     * 分页查询
     */
    List<ExpressFeeTemplate> findByPage(@Param("companyId") Long companyId,
                                        @Param("name") String name);

    /**
     * 根据ID查询
     */
    ExpressFeeTemplate findById(@Param("id") Long id);

    /**
     * 查询默认模板
     */
    ExpressFeeTemplate findDefault();

    /**
     * 新增
     */
    int insert(ExpressFeeTemplate template);

    /**
     * 修改
     */
    int update(ExpressFeeTemplate template);

    /**
     * 逻辑删除
     */
    int deleteById(@Param("id") Long id);

    /**
     * 取消其他默认模板
     */
    int clearDefault();
}
