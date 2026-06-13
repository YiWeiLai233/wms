package com.yiweilai.wms.express.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.exception.BusinessException;
import com.yiweilai.wms.exception.ErrorCode;
import com.yiweilai.wms.express.dto.ExpressFeeTemplateSaveDTO;
import com.yiweilai.wms.express.entity.ExpressCompany;
import com.yiweilai.wms.express.entity.ExpressFeeStep;
import com.yiweilai.wms.express.entity.ExpressFeeTemplate;
import com.yiweilai.wms.express.mapper.ExpressCompanyMapper;
import com.yiweilai.wms.express.mapper.ExpressFeeStepMapper;
import com.yiweilai.wms.express.mapper.ExpressFeeTemplateMapper;
import com.yiweilai.wms.express.service.ExpressFeeTemplateService;
import com.yiweilai.wms.express.vo.ExpressFeeTemplateVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 快递费用模板 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpressFeeTemplateServiceImpl implements ExpressFeeTemplateService {

    private final ExpressFeeTemplateMapper templateMapper;
    private final ExpressFeeStepMapper stepMapper;
    private final ExpressCompanyMapper companyMapper;

    @Override
    public List<ExpressFeeTemplateVO> findByCompanyId(Long companyId) {
        return templateMapper.findByCompanyId(companyId).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public PageResult<ExpressFeeTemplateVO> findByPage(Long companyId, String name, Integer page, Integer size) {
        PageHelper.startPage(page, size);
        List<ExpressFeeTemplate> list = templateMapper.findByPage(companyId, name);
        PageInfo<ExpressFeeTemplate> pageInfo = new PageInfo<>(list);

        List<ExpressFeeTemplateVO> voList = list.stream()
                .map(template -> {
                    ExpressFeeTemplateVO vo = convertToVO(template);
                    // 查询公司名称
                    ExpressCompany company = companyMapper.findById(template.getCompanyId());
                    if (company != null) {
                        vo.setCompanyName(company.getName());
                    }
                    return vo;
                })
                .collect(Collectors.toList());

        PageResult<ExpressFeeTemplateVO> result = new PageResult<>();
        result.setTotal(pageInfo.getTotal());
        result.setList(voList);
        return result;
    }

    @Override
    public ExpressFeeTemplateVO getById(Long id) {
        ExpressFeeTemplate template = templateMapper.findById(id);
        if (template == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "费用模板不存在");
        }

        ExpressFeeTemplateVO vo = convertToVO(template);

        // 查询公司名称
        ExpressCompany company = companyMapper.findById(template.getCompanyId());
        if (company != null) {
            vo.setCompanyName(company.getName());
        }

        // 查询阶梯配置
        List<ExpressFeeStep> steps = stepMapper.findByTemplateId(id);
        vo.setSteps(steps.stream().map(this::convertStepToVO).collect(Collectors.toList()));

        return vo;
    }

    @Override
    public ExpressFeeTemplateVO getDefault() {
        ExpressFeeTemplate template = templateMapper.findDefault();
        if (template == null) {
            return null;
        }
        return getById(template.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ExpressFeeTemplateSaveDTO dto) {
        // 如果设为默认，先取消其他默认
        if (dto.getIsDefault() != null && dto.getIsDefault() == 1) {
            templateMapper.clearDefault();
        }

        ExpressFeeTemplate template = new ExpressFeeTemplate();
        BeanUtils.copyProperties(dto, template);
        templateMapper.insert(template);

        // 保存阶梯配置
        if (dto.getSteps() != null && !dto.getSteps().isEmpty()) {
            for (ExpressFeeTemplateSaveDTO.FeeStepDTO stepDTO : dto.getSteps()) {
                ExpressFeeStep step = new ExpressFeeStep();
                step.setTemplateId(template.getId());
                step.setMinWeight(stepDTO.getMinWeight());
                step.setMaxWeight(stepDTO.getMaxWeight());
                step.setFee(stepDTO.getFee());
                step.setSortOrder(stepDTO.getSortOrder());
                stepMapper.insert(step);
            }
        }

        return template.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(ExpressFeeTemplateSaveDTO dto) {
        ExpressFeeTemplate template = templateMapper.findById(dto.getId());
        if (template == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "费用模板不存在");
        }

        // 如果设为默认，先取消其他默认
        if (dto.getIsDefault() != null && dto.getIsDefault() == 1) {
            templateMapper.clearDefault();
        }

        BeanUtils.copyProperties(dto, template);
        templateMapper.update(template);

        // 删除旧阶梯，重新插入
        stepMapper.deleteByTemplateId(template.getId());
        if (dto.getSteps() != null && !dto.getSteps().isEmpty()) {
            for (ExpressFeeTemplateSaveDTO.FeeStepDTO stepDTO : dto.getSteps()) {
                ExpressFeeStep step = new ExpressFeeStep();
                step.setTemplateId(template.getId());
                step.setMinWeight(stepDTO.getMinWeight());
                step.setMaxWeight(stepDTO.getMaxWeight());
                step.setFee(stepDTO.getFee());
                step.setSortOrder(stepDTO.getSortOrder());
                stepMapper.insert(step);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ExpressFeeTemplate template = templateMapper.findById(id);
        if (template == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "费用模板不存在");
        }
        templateMapper.deleteById(id);
        // 删除关联的阶梯配置
        stepMapper.deleteByTemplateId(id);
    }

    private ExpressFeeTemplateVO convertToVO(ExpressFeeTemplate template) {
        ExpressFeeTemplateVO vo = new ExpressFeeTemplateVO();
        BeanUtils.copyProperties(template, vo);
        return vo;
    }

    private ExpressFeeTemplateVO.FeeStepVO convertStepToVO(ExpressFeeStep step) {
        ExpressFeeTemplateVO.FeeStepVO vo = new ExpressFeeTemplateVO.FeeStepVO();
        BeanUtils.copyProperties(step, vo);
        return vo;
    }
}
