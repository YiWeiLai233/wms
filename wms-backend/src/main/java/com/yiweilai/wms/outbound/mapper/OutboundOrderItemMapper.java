package com.yiweilai.wms.outbound.mapper;

import com.yiweilai.wms.outbound.entity.OutboundOrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 出库明细 Mapper
 */
@Mapper
public interface OutboundOrderItemMapper {

    /**
     * 根据出库单ID查询明细
     */
    List<OutboundOrderItem> findByOutboundId(@Param("outboundId") Long outboundId);

    /**
     * 根据ID查询
     */
    OutboundOrderItem findById(@Param("id") Long id);

    /**
     * 根据出库单ID和SKU编码查询
     */
    OutboundOrderItem findByOutboundIdAndSkuCode(@Param("outboundId") Long outboundId,
                                                  @Param("skuCode") String skuCode);

    /**
     * 新增明细
     */
    int insert(OutboundOrderItem item);

    /**
     * 更新已拣数量
     */
    int updatePickedQty(@Param("id") Long id,
                        @Param("pickedQty") Integer pickedQty);

    /**
     * 更新扫码状态
     */
    int updateScanned(@Param("id") Long id,
                      @Param("scanned") Integer scanned);

    /**
     * 更新拣货货架
     */
    int updateShelfId(@Param("id") Long id,
                      @Param("shelfId") Long shelfId);

    /**
     * 根据出库单ID删除明细
     */
    int deleteByOutboundId(@Param("outboundId") Long outboundId);
}
