package com.yiweilai.wms.express.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 快递公司新增/修改参数
 */
@Data
public class ExpressCompanySaveDTO {

    /** 公司ID（修改时必填） */
    private Long id;

    /** 公司名称 */
    @NotBlank(message = "公司名称不能为空")
    private String name;

    /** 公司编码 */
    @NotBlank(message = "公司编码不能为空")
    private String code;

    /** 联系人 */
    private String contact;

    /** 联系电话 */
    private String phone;

    /** 状态：1-启用 0-禁用 */
    private Integer status = 1;
}
