package com.yiweilai.wms.platform.mapper;

import com.yiweilai.wms.platform.entity.Platform;
import com.yiweilai.wms.platform.vo.PlatformVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 平台 Mapper
 */
@Mapper
public interface PlatformMapper {

    /**
     * 分页查询平台列表
     */
    List<PlatformVO> findByPage(@Param("keyword") String keyword,
                                @Param("enabled") Integer enabled);

    /**
     * 根据ID查询
     */
    Platform findById(@Param("id") Long id);

    /**
     * 根据名称查询
     */
    Platform findByName(@Param("name") String name);

    /**
     * 查询启用的平台列表（用于下拉选择）
     */
    List<PlatformVO> findEnabledList();

    /**
     * 新增
     */
    int insert(Platform platform);

    /**
     * 更新
     */
    int update(Platform platform);

    /**
     * 逻辑删除
     */
    int deleteById(@Param("id") Long id);
}
