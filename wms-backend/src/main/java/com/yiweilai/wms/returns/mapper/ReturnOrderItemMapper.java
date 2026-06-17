package com.yiweilai.wms.returns.mapper;

import com.yiweilai.wms.returns.entity.ReturnOrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

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
     * 更新质检状态
     */
    int updateQualityStatus(@Param("id") Long id,
                            @Param("qualityStatus") String qualityStatus);

    /**
     * 更新质检状态和数量
     */
    int updateQualityStatusAndQuantity(@Param("id") Long id,
                                       @Param("qualityStatus") String qualityStatus,
                                       @Param("quantity") Integer quantity);

    /**
     * 根据退货单ID删除明细
     */
    int deleteByReturnId(@Param("returnId") Long returnId);

    /**
     * 统计某订单各SKU的已退货数量（排除已取消的退货单）
     */
    List<Map<String, Object>> sumReturnedQuantityByOrderId(@Param("orderId") Long orderId);
}
