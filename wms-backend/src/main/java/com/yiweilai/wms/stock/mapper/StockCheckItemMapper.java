package com.yiweilai.wms.stock.mapper;

import com.yiweilai.wms.stock.entity.StockCheckItem;
import com.yiweilai.wms.stock.vo.StockCheckItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 盘点明细 Mapper
 */
@Mapper
public interface StockCheckItemMapper {

    /**
     * 根据盘点单ID查询明细
     */
    List<StockCheckItem> findByCheckId(@Param("checkId") Long checkId);

    /**
     * 根据盘点单ID查询明细（含SKU编码和名称）
     */
    List<StockCheckItemVO> findVOByCheckId(@Param("checkId") Long checkId);

    /**
     * 根据ID查询
     */
    StockCheckItem findById(@Param("id") Long id);

    /**
     * 新增明细
     */
    int insert(StockCheckItem item);

    /**
     * 更新实际数量和差异
     */
    int updateActualQty(@Param("id") Long id,
                        @Param("actualQty") Integer actualQty,
                        @Param("diffQty") Integer diffQty);

    /**
     * 根据盘点单ID删除明细
     */
    int deleteByCheckId(@Param("checkId") Long checkId);
}
