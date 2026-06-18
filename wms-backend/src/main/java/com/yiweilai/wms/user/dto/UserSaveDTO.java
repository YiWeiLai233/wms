package com.yiweilai.wms.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 用户新增/修改参数
 */
@Data
public class UserSaveDTO {

    /** 用户ID（修改时必填） */
    private Long id;

    /** 用户名 */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 密码（新增时必填，修改时可选） */
    @Size(min = 4, message = "密码至少4位")
    private String password;

    /** 真实姓名 */
    private String realName;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 状态：0-禁用 1-启用 */
    private Integer status = 1;

    /** 角色ID列表 */
    @NotEmpty(message = "至少选择一个角色")
    private List<@NotNull(message = "角色不能为空") Long> roleIds;
}
