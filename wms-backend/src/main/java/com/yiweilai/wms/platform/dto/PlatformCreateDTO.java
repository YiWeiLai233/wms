package com.yiweilai.wms.platform.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 平台创建DTO
 */
@Data
public class PlatformCreateDTO {

    /** 平台名称 */
    @NotBlank(message = "平台名称不能为空")
    private String name;

    /** 展示颜色（十六进制） */
    private String color;

    /** 是否启用 */
    private Integer enabled = 1;

    /** 备注 */
    private String remark;
}
