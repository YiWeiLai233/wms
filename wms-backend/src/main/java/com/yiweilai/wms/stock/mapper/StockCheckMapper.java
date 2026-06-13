package com.yiweilai.wms.stock.mapper;

import com.yiweilai.wms.stock.entity.StockCheck;
import com.yiweilai.wms.stock.vo.StockCheckVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 盘点单 Mapper
 */
@Mapper
public interface StockCheckMapper {

    /**
     * 根据ID查询（含仓库名称）
     */
    StockCheckVO findById(@Param("id") Long id);

    /**
     * 根据盘点单号查询
     */
    StockCheck findByCheckNo(@Param("checkNo") String checkNo);

    /**
     * 新增盘点单
     */
    int insert(StockCheck stockCheck);

    /**
     * 更新状态
     */
    int updateStatus(@Param("id") Long id,
                     @Param("status") Integer status);

    /**
     * 分页查询（含仓库名称）
     */
    List<StockCheckVO> findByPage(@Param("warehouseId") Long warehouseId,
                                  @Param("status") Integer status,
                                  @Param("offset") int offset,
                                  @Param("size") int size);

    /**
     * 统计总数
     */
    long countByPage(@Param("warehouseId") Long warehouseId,
                     @Param("status") Integer status);
}
