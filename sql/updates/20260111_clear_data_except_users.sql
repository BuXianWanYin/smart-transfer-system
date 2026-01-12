-- ============================================
-- 数据库数据清理脚本（保留用户表数据）
-- ============================================
-- 说明：此脚本会清空除用户表外的所有数据
-- 用于统一使用新的逻辑（相对路径等）
-- 执行日期：2026-01-11
-- ============================================

USE smart_transfer;

-- 禁用外键检查（临时）
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================
-- 清空文件相关数据
-- ============================================

-- 清空回收站数据
TRUNCATE TABLE recovery_file;

-- 清空文件分片数据
TRUNCATE TABLE file_chunk;

-- 清空传输任务数据
TRUNCATE TABLE transfer_task;

-- 清空传输历史数据
TRUNCATE TABLE transfer_history;

-- 清空文件信息数据
TRUNCATE TABLE file_info;

-- 清空文件夹数据
TRUNCATE TABLE folder;

-- 清空拥塞控制指标数据
TRUNCATE TABLE congestion_metrics;

-- ============================================
-- 保留系统配置表（但清空拥塞控制配置，让系统重新初始化）
-- ============================================

-- 删除所有拥塞控制相关配置（系统会重新初始化）
DELETE FROM system_config WHERE config_key LIKE 'congestion.%';

-- ============================================
-- 重新启用外键检查
-- ============================================

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================
-- 验证清理结果
-- ============================================

SELECT '清理完成' AS status;
SELECT 
    'user' AS table_name,
    COUNT(*) AS record_count
FROM user
UNION ALL
SELECT 
    'file_info' AS table_name,
    COUNT(*) AS record_count
FROM file_info
UNION ALL
SELECT 
    'recovery_file' AS table_name,
    COUNT(*) AS record_count
FROM recovery_file
UNION ALL
SELECT 
    'folder' AS table_name,
    COUNT(*) AS record_count
FROM folder
UNION ALL
SELECT 
    'transfer_task' AS table_name,
    COUNT(*) AS record_count
FROM transfer_task
UNION ALL
SELECT 
    'transfer_history' AS table_name,
    COUNT(*) AS record_count
FROM transfer_history;
