# 数据库 SQL 脚本目录

此目录包含智能大文件传输系统的所有数据库 SQL 脚本。

## 📁 目录结构

```
sql/
├── init/                           # 初始化脚本（建表、初始数据）
│   └── 20260106_create_tables.sql # 创建所有数据表和初始配置
└── updates/                        # 数据库变更脚本
    └── (后续变更脚本按日期添加)
```

## 🗄️ 数据库信息

| 配置项 | 值 |
|--------|-----|
| 数据库名称 | `smart_transfer` |
| 主机地址 | `localhost` |
| 端口 | `3306` |
| 用户名 | `root` |
| 密码 | `123456` |
| 字符集 | `utf8mb4` |
| 排序规则 | `utf8mb4_unicode_ci` |

## 📊 数据表说明

### 1. t_file_info（文件信息表）
存储上传文件的基本信息。

**主要字段：**
- `id` - 主键ID
- `file_name` - 文件名
- `file_size` - 文件大小（字节）
- `file_hash` - 文件哈希值（MD5/SHA256）
- `file_path` - 文件存储路径
- `upload_status` - 上传状态（0-待上传 1-上传中 2-已完成）

### 2. t_file_chunk（文件分片表）
存储文件分片信息，用于断点续传。

**主要字段：**
- `id` - 主键ID
- `file_id` - 文件ID（外键）
- `chunk_number` - 分片序号
- `chunk_size` - 分片大小（字节）
- `chunk_hash` - 分片哈希值
- `upload_status` - 上传状态（0-未上传 1-已上传）

### 3. t_transfer_task（传输任务表）
记录文件传输任务的详细信息。

**主要字段：**
- `id` - 主键ID
- `task_id` - 任务唯一标识
- `file_id` - 文件ID（外键）
- `task_type` - 任务类型（1-上传 2-下载）
- `transfer_status` - 传输状态（0-待开始 1-传输中 2-已完成 3-失败 4-已暂停）
- `progress` - 传输进度（百分比）
- `transfer_speed` - 传输速率（字节/秒）

### 4. t_system_config（系统配置表）
存储系统配置参数，包括TCP拥塞控制参数。

**主要字段：**
- `id` - 主键ID
- `config_key` - 配置键
- `config_value` - 配置值
- `description` - 配置说明

**初始配置：**
- `congestion.algorithm` - 拥塞控制算法（ADAPTIVE）
- `congestion.initial_cwnd` - 初始拥塞窗口（10MB）
- `congestion.ssthresh` - 慢启动阈值（50MB）
- `congestion.max_cwnd` - 最大拥塞窗口（100MB）
- `congestion.min_cwnd` - 最小拥塞窗口（1MB）
- `transfer.chunk_size` - 文件分片大小（5MB）
- `transfer.max_file_size` - 最大文件大小（10GB）

### 5. t_congestion_metrics（拥塞控制指标表）
记录TCP拥塞控制算法的实时指标数据。

**主要字段：**
- `id` - 主键ID
- `task_id` - 传输任务ID（外键）
- `algorithm` - 拥塞控制算法（CUBIC/BBR/ADAPTIVE）
- `cwnd` - 拥塞窗口大小（字节）
- `ssthresh` - 慢启动阈值（字节）
- `rtt` - RTT往返时延（毫秒）
- `bandwidth` - 带宽（字节/秒）
- `loss_rate` - 丢包率（百分比）
- `record_time` - 记录时间

## 🚀 使用说明

### 方式一：使用 MCP MySQL 工具（推荐）

**⚠️ 重要：项目开发中所有数据库操作必须使用 MCP MySQL 工具！**

```javascript
// 1. 连接到 MySQL
mcp_mysql_connect_db({
  host: "localhost",
  user: "root",
  password: "123456",
  database: "mysql"
})

// 2. 创建数据库
mcp_mysql_execute({
  sql: "CREATE DATABASE IF NOT EXISTS smart_transfer DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
})

// 3. 切换到 smart_transfer 数据库
mcp_mysql_connect_db({
  host: "localhost",
  user: "root",
  password: "123456",
  database: "smart_transfer"
})

// 4. 依次执行建表SQL（从 init/20260106_create_tables.sql 复制）
// ...

// 5. 验证
mcp_mysql_list_tables()  // 查看所有表
mcp_mysql_query({ sql: "SELECT * FROM t_system_config" })  // 查看配置
```

### 方式二：使用命令行（备用）

```bash
# 1. 登录 MySQL
mysql -u root -p123456

# 2. 执行初始化脚本
source sql/init/20260106_create_tables.sql

# 3. 验证
USE smart_transfer;
SHOW TABLES;
SELECT * FROM t_system_config;
```

### 方式三：使用 MySQL 客户端工具（开发环境）

1. 打开 Navicat / DataGrip / MySQL Workbench
2. 连接到 MySQL（root/123456）
3. 打开并执行 `sql/init/20260106_create_tables.sql`
4. 刷新查看表结构

## 📝 数据库变更流程

当需要修改数据库结构时：

1. **创建变更脚本**
   ```bash
   # 在 sql/updates/ 目录创建新脚本
   # 命名格式：YYYYMMDD_description.sql
   # 例如：20260107_add_user_table.sql
   ```

2. **编写变更 SQL**
   ```sql
   -- 20260107_add_user_table.sql
   USE smart_transfer;
   
   -- 添加用户表
   CREATE TABLE t_user (
     id BIGINT PRIMARY KEY AUTO_INCREMENT,
     username VARCHAR(50) NOT NULL,
     password VARCHAR(100) NOT NULL,
     create_time DATETIME DEFAULT CURRENT_TIMESTAMP
   ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
   
   -- 为文件表添加用户ID字段
   ALTER TABLE t_file_info ADD COLUMN user_id BIGINT COMMENT '上传用户ID';
   ALTER TABLE t_file_info ADD CONSTRAINT fk_file_user FOREIGN KEY (user_id) REFERENCES t_user(id);
   ```

3. **使用 MCP 执行变更**
   ```javascript
   mcp_mysql_execute({
     sql: "CREATE TABLE t_user (...)"
   })
   
   mcp_mysql_execute({
     sql: "ALTER TABLE t_file_info ADD COLUMN user_id BIGINT"
   })
   ```

4. **验证变更结果**
   ```javascript
   mcp_mysql_describe_table({ table: "t_user" })
   mcp_mysql_describe_table({ table: "t_file_info" })
   ```

## ⚠️ 注意事项

1. **生产环境数据库密码**
   - 开发环境密码：`123456`
   - ⚠️ 生产环境必须修改为强密码！

2. **数据备份**
   - 在执行任何变更前，先备份数据库
   - 定期备份重要数据

3. **外键约束**
   - 删除父表记录时，子表记录会级联删除
   - 谨慎操作删除命令

4. **字符集**
   - 统一使用 `utf8mb4` 字符集
   - 支持 Emoji 和特殊字符

5. **索引优化**
   - 为常用查询字段添加索引
   - 定期分析慢查询日志

## 🔍 常用查询

```sql
-- 查看所有表
SHOW TABLES;

-- 查看表结构
DESC t_file_info;

-- 查看表创建语句
SHOW CREATE TABLE t_file_info;

-- 查看索引
SHOW INDEX FROM t_file_info;

-- 查看表数据量
SELECT 
  TABLE_NAME as '表名',
  TABLE_ROWS as '记录数',
  ROUND(DATA_LENGTH/1024/1024, 2) as '数据大小(MB)'
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'smart_transfer';

-- 查看系统配置
SELECT * FROM t_system_config;

-- 查看文件上传统计
SELECT 
  upload_status,
  COUNT(*) as count,
  SUM(file_size) as total_size
FROM t_file_info
GROUP BY upload_status;
```

## 📚 参考资料

- [MySQL 8.0 官方文档](https://dev.mysql.com/doc/refman/8.0/en/)
- [项目开发规范](.cursor/rules/project-development-rules.md)
- [完整项目开发指南](doc/完整项目开发指南.md)

