package com.yiweilai.wms.express.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 快递公司返回对象
 */
@Data
public class ExpressCompanyVO {

    /** 公司ID */
    private Long id;

    /** 公司名称 */
    private String name;

    /** 公司编码 */
    private String code;

    /** 联系人 */
    private String contact;

    /** 联系电话 */
    private String phone;

    /** 状态：1-启用 0-禁用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
