-- 文件夹软删除支持
-- 为 t_folder 表添加软删除相关字段

ALTER TABLE t_folder ADD COLUMN IF NOT EXISTS del_flag INT DEFAULT 0 COMMENT '删除标记 0-正常 1-已删除';
ALTER TABLE t_folder ADD COLUMN IF NOT EXISTS delete_batch_num VARCHAR(64) COMMENT '删除批次号';
ALTER TABLE t_folder ADD COLUMN IF NOT EXISTS delete_time DATETIME COMMENT '删除时间';

-- 为 t_file_info 表添加删除批次号字段
ALTER TABLE t_file_info ADD COLUMN IF NOT EXISTS delete_batch_num VARCHAR(64) COMMENT '删除批次号';

-- 为 t_recovery_file 表添加原文件夹ID字段
ALTER TABLE t_recovery_file ADD COLUMN IF NOT EXISTS original_folder_id BIGINT COMMENT '原文件夹ID（用于文件夹删除时记录）';

-- 添加索引以提高查询性能
CREATE INDEX IF NOT EXISTS idx_folder_del_flag ON t_folder(del_flag);
CREATE INDEX IF NOT EXISTS idx_folder_delete_batch_num ON t_folder(delete_batch_num);
CREATE INDEX IF NOT EXISTS idx_file_delete_batch_num ON t_file_info(delete_batch_num);
CREATE INDEX IF NOT EXISTS idx_recovery_original_folder_id ON t_recovery_file(original_folder_id);

