package com.yiweilai.wms.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
    @Size(min = 3, max = 50, message = "用户名长度3-50个字符")
    private String username;

    /** 密码（新增时必填，修改时可选） */
    @Size(min = 8, max = 100, message = "密码长度8-100个字符")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$", message = "密码必须包含大小写字母和数字")
    private String password;

    /** 真实姓名 */
    @Size(max = 50, message = "真实姓名最多50个字符")
    private String realName;

    /** 手机号 */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 邮箱 */
    @Size(max = 100, message = "邮箱最多100个字符")
    private String email;

    /** 状态：0-禁用 1-启用 */
    private Integer status = 1;

    /** 角色ID列表 */
    private List<Long> roleIds;
}
