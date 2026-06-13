package com.yiweilai.wms.express.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.exception.BusinessException;
import com.yiweilai.wms.exception.ErrorCode;
import com.yiweilai.wms.express.dto.ExpressCompanySaveDTO;
import com.yiweilai.wms.express.entity.ExpressCompany;
import com.yiweilai.wms.express.mapper.ExpressCompanyMapper;
import com.yiweilai.wms.express.service.ExpressCompanyService;
import com.yiweilai.wms.express.vo.ExpressCompanyVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 快递公司 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpressCompanyServiceImpl implements ExpressCompanyService {

    private final ExpressCompanyMapper companyMapper;

    @Override
    public List<ExpressCompanyVO> findAll() {
        return companyMapper.findAll().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public PageResult<ExpressCompanyVO> findByPage(String name, Integer status, Integer page, Integer size) {
        PageHelper.startPage(page, size);
        List<ExpressCompany> list = companyMapper.findByPage(name, status);
        PageInfo<ExpressCompany> pageInfo = new PageInfo<>(list);

        List<ExpressCompanyVO> voList = list.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        PageResult<ExpressCompanyVO> result = new PageResult<>();
        result.setTotal(pageInfo.getTotal());
        result.setList(voList);
        return result;
    }

    @Override
    public ExpressCompanyVO getById(Long id) {
        ExpressCompany company = companyMapper.findById(id);
        if (company == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "快递公司不存在");
        }
        return convertToVO(company);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ExpressCompanySaveDTO dto) {
        // 检查编码唯一性
        ExpressCompany existing = companyMapper.findByCode(dto.getCode());
        if (existing != null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "公司编码已存在");
        }

        ExpressCompany company = new ExpressCompany();
        BeanUtils.copyProperties(dto, company);
        companyMapper.insert(company);
        return company.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(ExpressCompanySaveDTO dto) {
        ExpressCompany company = companyMapper.findById(dto.getId());
        if (company == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "快递公司不存在");
        }

        // 检查编码唯一性（排除自身）
        if (!company.getCode().equals(dto.getCode())) {
            ExpressCompany existing = companyMapper.findByCode(dto.getCode());
            if (existing != null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "公司编码已存在");
            }
        }

        BeanUtils.copyProperties(dto, company);
        companyMapper.update(company);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ExpressCompany company = companyMapper.findById(id);
        if (company == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "快递公司不存在");
        }
        companyMapper.deleteById(id);
    }

    private ExpressCompanyVO convertToVO(ExpressCompany company) {
        ExpressCompanyVO vo = new ExpressCompanyVO();
        BeanUtils.copyProperties(company, vo);
        return vo;
    }
}
