-- 为 transfer_task 表增加 user_id 列，用于刷新/重登后恢复「我的」未完成任务列表
-- 上传任务：由 createTask 从 file_info 关联写入；下载任务：由下载接口从当前用户写入
ALTER TABLE transfer_task ADD COLUMN user_id BIGINT NULL COMMENT '用户ID（任务所属用户）';
