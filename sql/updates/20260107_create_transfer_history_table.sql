-- 创建传输历史记录表
-- 创建时间: 2026-01-07
-- 说明: 用于存储文件传输的历史记录，支持上传和下载记录

CREATE TABLE IF NOT EXISTS t_transfer_history (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  task_id VARCHAR(64) NOT NULL COMMENT '传输任务ID',
  file_id BIGINT NOT NULL COMMENT '文件ID',
  file_name VARCHAR(255) NOT NULL COMMENT '文件名',
  file_size BIGINT NOT NULL COMMENT '文件大小（字节）',
  file_hash VARCHAR(64) COMMENT '文件哈希值',
  transfer_type VARCHAR(20) NOT NULL DEFAULT 'UPLOAD' COMMENT '传输类型：UPLOAD-上传, DOWNLOAD-下载',
  transfer_status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED' COMMENT '传输状态：COMPLETED-成功, FAILED-失败',
  avg_speed BIGINT DEFAULT 0 COMMENT '平均传输速度（字节/秒）',
  duration INT DEFAULT 0 COMMENT '传输时长（秒）',
  algorithm VARCHAR(50) COMMENT '使用的拥塞控制算法：CUBIC, BBR, ADAPTIVE',
  completed_time DATETIME COMMENT '完成时间',
  error_message VARCHAR(500) COMMENT '错误信息（失败时记录）',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_file_id (file_id) COMMENT '文件ID索引',
  INDEX idx_task_id (task_id) COMMENT '任务ID索引',
  INDEX idx_transfer_type (transfer_type) COMMENT '传输类型索引',
  INDEX idx_completed_time (completed_time) COMMENT '完成时间索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='传输历史记录表';

-- 插入示例数据（可选）
-- INSERT INTO t_transfer_history (task_id, file_id, file_name, file_size, file_hash, transfer_type, transfer_status, avg_speed, duration, algorithm, completed_time)
-- VALUES ('TASK001', 1, 'example.pdf', 1048576, 'abc123hash', 'UPLOAD', 'COMPLETED', 2097152, 5, 'CUBIC', NOW());

