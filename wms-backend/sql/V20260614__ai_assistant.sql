CREATE TABLE IF NOT EXISTS ai_conversation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '会话ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    title VARCHAR(255) DEFAULT NULL COMMENT '会话标题',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_user_id (user_id),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI会话表';

CREATE TABLE IF NOT EXISTS ai_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
    conversation_id BIGINT NOT NULL COMMENT '会话ID',
    user_id BIGINT DEFAULT NULL COMMENT '用户ID',
    role VARCHAR(30) NOT NULL COMMENT '角色：user/assistant/system/tool',
    content TEXT NOT NULL COMMENT '消息内容',
    metadata JSON DEFAULT NULL COMMENT '元数据',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_conversation_id (conversation_id),
    KEY idx_user_id (user_id),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI消息表';

CREATE TABLE IF NOT EXISTS ai_knowledge_document (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '知识库文档ID',
    title VARCHAR(255) NOT NULL COMMENT '标题',
    file_id BIGINT DEFAULT NULL COMMENT '关联file_record.id',
    file_name VARCHAR(255) DEFAULT NULL COMMENT '文件名',
    file_path VARCHAR(500) DEFAULT NULL COMMENT '文件路径',
    source_type VARCHAR(50) NOT NULL DEFAULT 'FILE' COMMENT '来源类型',
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/PROCESSING/SUCCESS/FAILED',
    chunk_count INT NOT NULL DEFAULT 0 COMMENT '分块数量',
    error_message TEXT DEFAULT NULL COMMENT '错误信息',
    created_by BIGINT DEFAULT NULL COMMENT '创建人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_status (status),
    KEY idx_created_by (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI知识库文档表';

CREATE TABLE IF NOT EXISTS ai_knowledge_chunk (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '知识片段ID',
    document_id BIGINT NOT NULL COMMENT '文档ID',
    chunk_index INT NOT NULL COMMENT '分块序号',
    content TEXT NOT NULL COMMENT '文本内容',
    vector_id VARCHAR(100) DEFAULT NULL COMMENT 'Qdrant向量ID',
    metadata JSON DEFAULT NULL COMMENT '元数据',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_document_id (document_id),
    KEY idx_vector_id (vector_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI知识库分块表';

CREATE TABLE IF NOT EXISTS ai_tool_call_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '工具调用日志ID',
    user_id BIGINT DEFAULT NULL COMMENT '用户ID',
    conversation_id BIGINT DEFAULT NULL COMMENT '会话ID',
    message_id BIGINT DEFAULT NULL COMMENT '消息ID',
    tool_name VARCHAR(100) NOT NULL COMMENT '工具名称',
    request_params JSON DEFAULT NULL COMMENT '请求参数',
    response_data JSON DEFAULT NULL COMMENT '响应数据',
    status VARCHAR(50) NOT NULL COMMENT '状态：SUCCESS/FAILED',
    error_message TEXT DEFAULT NULL COMMENT '错误信息',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_user_id (user_id),
    KEY idx_conversation_id (conversation_id),
    KEY idx_tool_name (tool_name),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI工具调用日志表';
