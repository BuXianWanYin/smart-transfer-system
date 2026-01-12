-- ============================================
-- 智能大文件传输系统 - 数据库初始化脚本
-- ============================================
-- 数据库名称: smart_transfer
-- 字符集: utf8mb4
-- 排序规则: utf8mb4_unicode_ci
-- 创建日期: 2026-01-06
-- ============================================

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS smart_transfer 
DEFAULT CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE smart_transfer;

-- ============================================
-- 表1: file_info（文件信息表）
-- ============================================
CREATE TABLE IF NOT EXISTS file_info (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  file_name VARCHAR(255) NOT NULL COMMENT '文件名',
  file_size BIGINT NOT NULL COMMENT '文件大小（字节）',
  file_hash VARCHAR(64) NOT NULL COMMENT '文件哈希值（MD5/SHA256）',
  file_path VARCHAR(500) NOT NULL COMMENT '文件存储路径',
  upload_status INT DEFAULT 0 COMMENT '上传状态：0-待上传 1-上传中 2-已完成',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_file_hash (file_hash),
  INDEX idx_upload_status (upload_status),
  INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件信息表';

-- ============================================
-- 表2: file_chunk（文件分片表）
-- ============================================
CREATE TABLE IF NOT EXISTS file_chunk (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  file_id BIGINT NOT NULL COMMENT '文件ID',
  chunk_number INT NOT NULL COMMENT '分片序号（从0开始）',
  chunk_size BIGINT NOT NULL COMMENT '分片大小（字节）',
  chunk_hash VARCHAR(64) COMMENT '分片哈希值',
  upload_status INT DEFAULT 0 COMMENT '上传状态：0-未上传 1-已上传',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX idx_file_id (file_id),
  INDEX idx_chunk_number (chunk_number),
  UNIQUE KEY uk_file_chunk (file_id, chunk_number),
  FOREIGN KEY (file_id) REFERENCES file_info(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件分片表';

-- ============================================
-- 表3: transfer_task（传输任务表）
-- ============================================
CREATE TABLE IF NOT EXISTS transfer_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  task_id VARCHAR(64) NOT NULL UNIQUE COMMENT '任务唯一标识',
  file_id BIGINT NOT NULL COMMENT '文件ID',
  task_type INT NOT NULL COMMENT '任务类型：1-上传 2-下载',
  transfer_status INT DEFAULT 0 COMMENT '传输状态：0-待开始 1-传输中 2-已完成 3-失败 4-已暂停',
  progress DECIMAL(5,2) DEFAULT 0.00 COMMENT '传输进度（百分比）',
  transfer_speed BIGINT DEFAULT 0 COMMENT '传输速率（字节/秒）',
  start_time DATETIME COMMENT '开始时间',
  end_time DATETIME COMMENT '结束时间',
  error_message VARCHAR(500) COMMENT '错误信息',
  INDEX idx_task_id (task_id),
  INDEX idx_file_id (file_id),
  INDEX idx_transfer_status (transfer_status),
  FOREIGN KEY (file_id) REFERENCES file_info(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='传输任务表';

-- ============================================
-- 表4: system_config（系统配置表）
-- ============================================
CREATE TABLE IF NOT EXISTS system_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  config_key VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键',
  config_value VARCHAR(500) NOT NULL COMMENT '配置值',
  description VARCHAR(255) COMMENT '配置说明',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- ============================================
-- 表5: congestion_metrics（拥塞控制指标表）
-- ============================================
CREATE TABLE IF NOT EXISTS congestion_metrics (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  task_id VARCHAR(64) NOT NULL COMMENT '传输任务ID',
  algorithm VARCHAR(20) NOT NULL COMMENT '拥塞控制算法：CUBIC/BBR/ADAPTIVE',
  cwnd BIGINT NOT NULL COMMENT '拥塞窗口大小（字节）',
  ssthresh BIGINT COMMENT '慢启动阈值（字节）',
  rtt BIGINT COMMENT 'RTT往返时延（毫秒）',
  bandwidth BIGINT COMMENT '带宽（字节/秒）',
  loss_rate DECIMAL(5,2) COMMENT '丢包率（百分比）',
  record_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
  INDEX idx_task_id (task_id),
  INDEX idx_record_time (record_time),
  FOREIGN KEY (task_id) REFERENCES transfer_task(task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拥塞控制指标表';

-- ============================================
-- 初始化系统配置数据
-- ============================================
INSERT INTO system_config (config_key, config_value, description) VALUES 
('congestion.algorithm', 'ADAPTIVE', '拥塞控制算法类型：CUBIC/BBR/ADAPTIVE'),
('congestion.initial_cwnd', '10485760', '初始拥塞窗口大小（字节）：10MB'),
('congestion.ssthresh', '52428800', '慢启动阈值（字节）：50MB'),
('congestion.max_cwnd', '104857600', '最大拥塞窗口（字节）：100MB'),
('congestion.min_cwnd', '1048576', '最小拥塞窗口（字节）：1MB'),
('congestion.max_rate', '0', '最大传输速率（字节/秒，0表示不限制）'),
('congestion.min_rate', '1048576', '最小传输速率（字节/秒）：1MB/s'),
('transfer.chunk_size', '5242880', '文件分片大小（字节）：5MB'),
('transfer.max_file_size', '10737418240', '最大文件大小（字节）：10GB');

-- ============================================
-- 脚本执行完成
-- ============================================
SELECT '数据库初始化完成！' AS message;
SELECT COUNT(*) AS table_count FROM information_schema.tables WHERE table_schema = 'smart_transfer';
SELECT COUNT(*) AS config_count FROM system_config;

