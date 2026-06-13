package com.yiweilai.wms.express.service;

import com.yiweilai.wms.express.dto.ExpressQueryDTO;
import com.yiweilai.wms.express.vo.ExpressInfoVO;

/**
 * 快递 Service
 */
public interface ExpressService {

    /**
     * 查询快递信息
     * @param trackingNo 快递单号
     * @param carrier 快递公司编码（可选）
     * @return 快递信息，如果无法查询返回null
     */
    ExpressInfoVO query(String trackingNo, String carrier);

    /**
     * 计算快递费用
     * @param dto 查询参数
     * @return 费用信息
     */
    ExpressInfoVO.FeeInfo calculateFee(ExpressQueryDTO dto);
}
