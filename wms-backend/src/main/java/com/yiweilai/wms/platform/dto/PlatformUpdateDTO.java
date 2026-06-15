package com.yiweilai.wms.platform.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 平台更新DTO
 */
@Data
public class PlatformUpdateDTO {

    /** 平台ID（由路径参数设置） */
    private Long id;

    /** 平台名称 */
    @NotBlank(message = "平台名称不能为空")
    private String name;

    /** 展示颜色（十六进制） */
    private String color;

    /** 是否启用 */
    private Integer enabled;

    /** 备注 */
    private String remark;
}
