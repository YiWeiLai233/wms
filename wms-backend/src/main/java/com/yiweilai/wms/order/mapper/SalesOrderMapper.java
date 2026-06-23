package com.yiweilai.wms.order.mapper;

import com.yiweilai.wms.order.entity.SalesOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单 Mapper
 */
@Mapper
public interface SalesOrderMapper {

    /**
     * 分页查询订单
     */
    List<SalesOrder> findByPage(@Param("orderNo") String orderNo,
                                @Param("platformOrderNo") String platformOrderNo,
                                @Param("receiverNameHash") String receiverNameHash,
                                @Param("receiverPhoneHash") String receiverPhoneHash,
                                @Param("orderStatus") String orderStatus,
                                @Param("warehouseId") Long warehouseId);

    /**
     * Search by non-sensitive keyword and exact privacy hashes.
     */
    List<SalesOrder> searchByKeyword(@Param("keyword") String keyword,
                                     @Param("receiverNameHash") String receiverNameHash,
                                     @Param("receiverPhoneHash") String receiverPhoneHash,
                                     @Param("orderStatus") String orderStatus,
                                     @Param("warehouseId") Long warehouseId);

    /**
     * 根据ID查询
     */
    SalesOrder findById(@Param("id") Long id);

    /**
     * 根据订单号查询
     */
    SalesOrder findByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 根据平台单号查询
     */
    SalesOrder findByPlatformOrderNo(@Param("platformOrderNo") String platformOrderNo);

    /**
     * 新增订单
     */
    int insert(SalesOrder order);

    /**
     * 更新订单状态
     */
    int updateStatus(@Param("id") Long id,
                     @Param("orderStatus") String orderStatus);

    /**
     * 更新发货时间
     */
    int updateShippedAt(@Param("id") Long id);

    /**
     * 更新订单信息
     */
    int update(SalesOrder order);

    /**
     * Batch query for privacy migration.
     */
    List<SalesOrder> findPrivacyMigrationBatch(@Param("lastId") Long lastId,
                                               @Param("limit") Integer limit);

    /**
     * Update encrypted receiver fields and lookup hashes.
     */
    int updatePrivacyFields(SalesOrder order);

    /**
     * 逻辑删除订单
     */
    int softDelete(@Param("id") Long id);
}
