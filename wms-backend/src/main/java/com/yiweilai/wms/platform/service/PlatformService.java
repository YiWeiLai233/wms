package com.yiweilai.wms.platform.service;

import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.platform.dto.PlatformCreateDTO;
import com.yiweilai.wms.platform.dto.PlatformUpdateDTO;
import com.yiweilai.wms.platform.vo.PlatformVO;

import java.util.List;

/**
 * 平台 Service 接口
 */
public interface PlatformService {

    /**
     * 分页查询平台列表
     */
    PageResult<PlatformVO> findByPage(String keyword, Integer enabled, Integer page, Integer size);

    /**
     * 根据ID查询平台详情
     */
    PlatformVO getById(Long id);

    /**
     * 查询启用的平台列表（用于下拉选择）
     */
    List<PlatformVO> findEnabledList();

    /**
     * 创建平台
     */
    Long create(PlatformCreateDTO dto);

    /**
     * 更新平台
     */
    void update(PlatformUpdateDTO dto);

    /**
     * 删除平台
     */
    void delete(Long id);
}
