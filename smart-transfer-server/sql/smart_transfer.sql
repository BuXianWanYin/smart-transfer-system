/*
 Navicat Premium Dump SQL

 Source Server         : mysql8
 Source Server Type    : MySQL
 Source Server Version : 80041 (8.0.41)
 Source Host           : localhost:3306
 Source Schema         : smart_transfer

 Target Server Type    : MySQL
 Target Server Version : 80041 (8.0.41)
 File Encoding         : 65001

 Date: 03/02/2026 06:22:45
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for congestion_metrics
-- ----------------------------
DROP TABLE IF EXISTS `congestion_metrics`;
CREATE TABLE `congestion_metrics`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '传输任务ID（关联 t_transfer_task.task_id）',
  `algorithm` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '拥塞控制算法：CUBIC/BBR/ADAPTIVE',
  `cwnd` bigint NOT NULL COMMENT '拥塞窗口大小（字节）',
  `ssthresh` bigint NULL DEFAULT NULL COMMENT '慢启动阈值（字节）',
  `rtt` bigint NULL DEFAULT NULL COMMENT 'RTT往返时延（毫秒）',
  `bandwidth` bigint NULL DEFAULT NULL COMMENT '带宽（字节/秒）',
  `loss_rate` decimal(5, 2) NULL DEFAULT NULL COMMENT '丢包率（百分比）',
  `record_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_task_id`(`task_id` ASC) USING BTREE,
  INDEX `idx_record_time`(`record_time` ASC) USING BTREE,
  CONSTRAINT `congestion_metrics_ibfk_1` FOREIGN KEY (`task_id`) REFERENCES `transfer_task` (`task_id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 9343 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '拥塞控制指标表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for file_chunk
-- ----------------------------
DROP TABLE IF EXISTS `file_chunk`;
CREATE TABLE `file_chunk`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `file_id` bigint NOT NULL COMMENT '文件ID',
  `chunk_number` int NOT NULL COMMENT '分片序号（从0开始）',
  `chunk_size` bigint NOT NULL COMMENT '分片大小（字节）',
  `chunk_hash` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '分片哈希值',
  `upload_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'PENDING' COMMENT '上传状态：PENDING-待上传 UPLOADING-上传中 COMPLETED-已完成',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_file_chunk`(`file_id` ASC, `chunk_number` ASC) USING BTREE,
  INDEX `idx_file_id`(`file_id` ASC) USING BTREE,
  INDEX `idx_chunk_number`(`chunk_number` ASC) USING BTREE,
  CONSTRAINT `file_chunk_ibfk_1` FOREIGN KEY (`file_id`) REFERENCES `file_info` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 17174 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '文件分片表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for file_info
-- ----------------------------
DROP TABLE IF EXISTS `file_info`;
CREATE TABLE `file_info`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件名',
  `extend_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '文件扩展名',
  `file_size` bigint NOT NULL COMMENT '文件大小（字节）',
  `file_hash` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件哈希值（MD5/SHA256）',
  `file_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件存储路径',
  `is_dir` tinyint(1) NULL DEFAULT 0 COMMENT '是否目录（0文件 1目录）',
  `upload_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'PENDING' COMMENT '上传状态：PENDING-待上传 UPLOADING-上传中 COMPLETED-已完成',
  `del_flag` tinyint(1) NULL DEFAULT 0 COMMENT '删除标志（0正常 1已删除）',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `folder_id` bigint NULL DEFAULT 0 COMMENT '所属文件夹ID，0表示根目录',
  `delete_batch_num` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '删除批次号',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_file_hash`(`file_hash` ASC) USING BTREE,
  INDEX `idx_upload_status`(`upload_status` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_folder_id`(`folder_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_file_delete_batch_num`(`delete_batch_num` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 38 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '文件信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for folder
-- ----------------------------
DROP TABLE IF EXISTS `folder`;
CREATE TABLE `folder`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '文件夹ID',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `folder_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件夹名称',
  `parent_id` bigint NULL DEFAULT 0 COMMENT '父文件夹ID，0表示根目录',
  `path` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '/' COMMENT '文件夹路径',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` int NULL DEFAULT 0 COMMENT '删除标记 0-正常 1-已删除',
  `delete_batch_num` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '删除批次号',
  `delete_time` datetime NULL DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_parent_id`(`parent_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_folder_del_flag`(`del_flag` ASC) USING BTREE,
  INDEX `idx_folder_delete_batch_num`(`delete_batch_num` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '文件夹表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for recovery_file
-- ----------------------------
DROP TABLE IF EXISTS `recovery_file`;
CREATE TABLE `recovery_file`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '回收站记录ID',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `file_id` bigint NULL DEFAULT NULL COMMENT '文件ID（文件夹为NULL）',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件名',
  `extend_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '文件扩展名',
  `file_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '原文件路径',
  `folder_id` bigint NULL DEFAULT 0 COMMENT '原所属文件夹ID',
  `is_dir` tinyint(1) NULL DEFAULT 0 COMMENT '是否目录',
  `file_size` bigint NULL DEFAULT 0 COMMENT '文件大小',
  `delete_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '删除时间',
  `delete_batch_num` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '删除批次号（批量删除用）',
  `original_folder_id` bigint NULL DEFAULT NULL COMMENT '原文件夹ID（用于文件夹删除时记录）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_file_id`(`file_id` ASC) USING BTREE,
  INDEX `idx_delete_time`(`delete_time` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 15 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '回收站文件表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for system_activity
-- ----------------------------
DROP TABLE IF EXISTS `system_activity`;
CREATE TABLE `system_activity`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '活动ID',
  `activity_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '活动类型：USER_REGISTER-用户注册, FILE_UPLOAD-文件上传, FILE_DOWNLOAD-文件下载, SYSTEM_CONFIG-系统配置',
  `activity_desc` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '活动描述',
  `related_user_id` bigint NULL DEFAULT NULL COMMENT '关联用户ID',
  `related_user_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '关联用户名',
  `activity_data` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '活动数据（JSON格式）',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_type`(`activity_type` ASC) USING BTREE,
  INDEX `idx_user`(`related_user_id` ASC) USING BTREE,
  INDEX `idx_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统活动记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for system_config
-- ----------------------------
DROP TABLE IF EXISTS `system_config`;
CREATE TABLE `system_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `config_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '配置键',
  `config_value` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '配置值',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '配置说明',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `config_key`(`config_key` ASC) USING BTREE,
  INDEX `idx_config_key`(`config_key` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 31 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for transfer_history
-- ----------------------------
DROP TABLE IF EXISTS `transfer_history`;
CREATE TABLE `transfer_history`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `task_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '传输任务ID',
  `file_id` bigint NULL DEFAULT NULL COMMENT '文件ID',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件名',
  `file_size` bigint NOT NULL COMMENT '文件大小（字节）',
  `file_hash` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '文件哈希值',
  `transfer_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'UPLOAD' COMMENT '传输类型：UPLOAD-上传, DOWNLOAD-下载',
  `transfer_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'COMPLETED' COMMENT '传输状态：COMPLETED-成功, FAILED-失败',
  `avg_speed` bigint NULL DEFAULT 0 COMMENT '平均传输速度（字节/秒）',
  `duration` int NULL DEFAULT 0 COMMENT '传输时长（秒）',
  `algorithm` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '使用的拥塞控制算法：CUBIC, BBR, ADAPTIVE',
  `completed_time` datetime NULL DEFAULT NULL COMMENT '完成时间',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '错误信息（失败时记录）',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_file_id`(`file_id` ASC) USING BTREE COMMENT '文件ID索引',
  INDEX `idx_task_id`(`task_id` ASC) USING BTREE COMMENT '任务ID索引',
  INDEX `idx_transfer_type`(`transfer_type` ASC) USING BTREE COMMENT '传输类型索引',
  INDEX `idx_completed_time`(`completed_time` ASC) USING BTREE COMMENT '完成时间索引',
  INDEX `idx_transfer_history_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 36 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '传输历史记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for transfer_task
-- ----------------------------
DROP TABLE IF EXISTS `transfer_task`;
CREATE TABLE `transfer_task`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务唯一标识',
  `file_id` bigint NOT NULL COMMENT '文件ID',
  `task_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'UPLOAD' COMMENT '任务类型：UPLOAD-上传 DOWNLOAD-下载',
  `transfer_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'PENDING' COMMENT '传输状态：PENDING-待处理 PROCESSING-处理中 COMPLETED-已完成 FAILED-失败 PAUSED-已暂停',
  `progress` decimal(5, 2) NULL DEFAULT 0.00 COMMENT '传输进度（百分比）',
  `transfer_speed` bigint NULL DEFAULT 0 COMMENT '传输速率（字节/秒）',
  `start_time` datetime NULL DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime NULL DEFAULT NULL COMMENT '结束时间',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '错误信息',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID（任务所属用户）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `task_id`(`task_id` ASC) USING BTREE,
  INDEX `idx_task_id`(`task_id` ASC) USING BTREE,
  INDEX `idx_file_id`(`file_id` ASC) USING BTREE,
  INDEX `idx_transfer_status`(`transfer_status` ASC) USING BTREE,
  CONSTRAINT `transfer_task_ibfk_1` FOREIGN KEY (`file_id`) REFERENCES `file_info` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 39 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '传输任务表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户名',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码（加密）',
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '昵称',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '头像URL',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '手机号',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `last_login_time` datetime NULL DEFAULT NULL COMMENT '最后登录时间',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'USER' COMMENT '用户角色：ADMIN-管理员，USER-普通用户',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_username`(`username` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户表' ROW_FORMAT = Dynamic;


SET FOREIGN_KEY_CHECKS = 1;
