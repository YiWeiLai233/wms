package com.yiweilai.wms.express.mapper;

import com.yiweilai.wms.express.entity.ExpressCompany;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 快递公司 Mapper
 */
@Mapper
public interface ExpressCompanyMapper {

    /**
     * 查询所有启用的公司
     */
    List<ExpressCompany> findAll();

    /**
     * 分页查询
     */
    List<ExpressCompany> findByPage(@Param("name") String name,
                                    @Param("status") Integer status);

    /**
     * 根据ID查询
     */
    ExpressCompany findById(@Param("id") Long id);

    /**
     * 根据编码查询
     */
    ExpressCompany findByCode(@Param("code") String code);

    /**
     * 新增
     */
    int insert(ExpressCompany company);

    /**
     * 修改
     */
    int update(ExpressCompany company);

    /**
     * 逻辑删除
     */
    int deleteById(@Param("id") Long id);
}
