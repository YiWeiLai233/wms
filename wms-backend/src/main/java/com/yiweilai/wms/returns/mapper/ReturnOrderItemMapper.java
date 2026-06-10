package com.yiweilai.wms.returns.mapper;

import com.yiweilai.wms.returns.entity.ReturnOrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 退货明细 Mapper
 */
@Mapper
public interface ReturnOrderItemMapper {

    /**
     * 根据退货单ID查询明细
     */
    List<ReturnOrderItem> findByReturnId(@Param("returnId") Long returnId);

    /**
     * 根据ID查询
     */
    ReturnOrderItem findById(@Param("id") Long id);

    /**
     * 新增明细
     */
    int insert(ReturnOrderItem item);

    /**
     * 更新质检状态和库位
     */
    int updateQualityStatus(@Param("id") Long id,
                            @Param("qualityStatus") String qualityStatus,
                            @Param("locationId") Long locationId);

    /**
     * 根据退货单ID删除明细
     */
    int deleteByReturnId(@Param("returnId") Long returnId);
}
