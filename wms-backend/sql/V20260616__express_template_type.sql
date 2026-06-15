-- 快递费用模板添加类型字段
ALTER TABLE express_fee_template 
ADD COLUMN template_type VARCHAR(20) DEFAULT 'LADDER' COMMENT '模板类型：LADDER-阶梯计费, FIRST_CONTINUE-首重续重' AFTER name,
ADD COLUMN first_weight DECIMAL(10,2) DEFAULT NULL COMMENT '首重重量(kg)' AFTER template_type,
ADD COLUMN first_fee DECIMAL(10,2) DEFAULT NULL COMMENT '首重费用(元)' AFTER first_weight,
ADD COLUMN additional_weight DECIMAL(10,2) DEFAULT NULL COMMENT '续重重量(kg)' AFTER first_fee,
ADD COLUMN additional_fee DECIMAL(10,2) DEFAULT NULL COMMENT '续重费用(元/kg)' AFTER additional_weight;
