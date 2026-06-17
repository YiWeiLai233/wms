package com.yiweilai.wms.platform.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.config.CacheService;
import com.yiweilai.wms.exception.BusinessException;
import com.yiweilai.wms.exception.ErrorCode;
import com.yiweilai.wms.platform.dto.PlatformCreateDTO;
import com.yiweilai.wms.platform.dto.PlatformUpdateDTO;
import com.yiweilai.wms.platform.entity.Platform;
import com.yiweilai.wms.platform.mapper.PlatformMapper;
import com.yiweilai.wms.platform.service.PlatformService;
import com.yiweilai.wms.platform.vo.PlatformVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 平台 Service 实现
 */
@Service
@RequiredArgsConstructor
public class PlatformServiceImpl implements PlatformService {

    private final PlatformMapper platformMapper;
    private final CacheService cacheService;

    private static final String CACHE_KEY_PLATFORMS_ENABLED = "cache:platforms:enabled";

    @Override
    public PageResult<PlatformVO> findByPage(String keyword, Integer enabled, Integer page, Integer size) {
        PageHelper.startPage(page, size);
        List<PlatformVO> list = platformMapper.findByPage(keyword, enabled);
        PageInfo<PlatformVO> pageInfo = new PageInfo<>(list);

        PageResult<PlatformVO> result = new PageResult<>();
        result.setTotal(pageInfo.getTotal());
        result.setList(list);
        return result;
    }

    @Override
    public PlatformVO getById(Long id) {
        Platform platform = platformMapper.findById(id);
        if (platform == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "平台不存在");
        }

        PlatformVO vo = new PlatformVO();
        BeanUtils.copyProperties(platform, vo);
        return vo;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<PlatformVO> findEnabledList() {
        return cacheService.getOrLoad(CACHE_KEY_PLATFORMS_ENABLED, 30, TimeUnit.MINUTES,
                () -> platformMapper.findEnabledList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(PlatformCreateDTO dto) {
        Platform platform = new Platform();
        BeanUtils.copyProperties(dto, platform);
        if (platform.getEnabled() == null) {
            platform.setEnabled(1);
        }
        platformMapper.insert(platform);
        cacheService.delete(CACHE_KEY_PLATFORMS_ENABLED);
        return platform.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(PlatformUpdateDTO dto) {
        Platform platform = platformMapper.findById(dto.getId());
        if (platform == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "平台不存在");
        }

        BeanUtils.copyProperties(dto, platform);
        platformMapper.update(platform);
        cacheService.delete(CACHE_KEY_PLATFORMS_ENABLED);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Platform platform = platformMapper.findById(id);
        if (platform == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "平台不存在");
        }

        platformMapper.deleteById(id);
        cacheService.delete(CACHE_KEY_PLATFORMS_ENABLED);
    }
}
