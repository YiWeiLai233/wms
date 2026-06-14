package com.yiweilai.wms.ai.mapper;

import com.yiweilai.wms.ai.entity.AiToolCallLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiToolCallLogMapper {

    int insert(AiToolCallLog log);
}
