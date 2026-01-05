# 基于Java+Vue的TCP拥塞控制优化大文件传输工具 - 后端项目

## 项目简介

基于Java+Vue的TCP拥塞控制优化大文件传输工具后端服务，采用 Spring Boot 2.6.13 + MySQL 8.0 + Redis 构建，实现了 TCP 拥塞控制算法优化的大文件传输功能。

## 技术栈

- **Spring Boot 2.6.13** - 应用框架
- **Spring Data JPA** - ORM 框架
- **MySQL 8.0** - 关系型数据库
- **Redis** - 缓存和分布式锁
- **Netty 4.1** - 高性能网络框架
- **Lombok** - 简化 Java 代码
- **Guava** - Google 工具库（限流器）
- **Commons Math3** - 数学计算库
- **Micrometer** - 监控指标
- **Maven** - 项目构建工具

## 核心功能

### 1. TCP 拥塞控制算法
- **CUBIC 算法**：Linux 默认拥塞控制算法
- **BBR 算法**：Google 开发的基于带宽延迟的算法
- **ADAPTIVE 算法**：自适应算法，根据网络状况动态选择

### 2. 文件传输功能
- **分片上传**：支持大文件分片上传
- **断点续传**：支持文件上传中断后继续传输
- **文件合并**：自动合并分片为完整文件
- **完整性校验**：MD5/SHA256 校验保证文件完整性

### 3. 重传机制
- **超时重传**：网络超时自动重传
- **快速重传**：检测到丢包立即重传
- **选择性重传**：只重传丢失的分片

## 项目结构

```
smart-transfer-server/
├── src/
│   ├── main/
│   │   ├── java/com/server/smarttransferserver/
│   │   │   ├── config/              # 配置类
│   │   │   ├── controller/          # 控制器
│   │   │   ├── entity/              # 实体类
│   │   │   ├── repository/          # 数据访问层
│   │   │   ├── service/             # 业务逻辑层
│   │   │   ├── dto/                 # 数据传输对象
│   │   │   ├── vo/                  # 视图对象
│   │   │   ├── enums/               # 枚举
│   │   │   ├── exception/           # 异常处理
│   │   │   ├── utils/               # 工具类
│   │   │   ├── congestion/          # 拥塞控制算法
│   │   │   └── netty/               # Netty 网络层
│   │   └── resources/
│   │       ├── application.yml      # 应用配置
│   │       └── logback-spring.xml   # 日志配置
│   └── test/                        # 测试代码
├── file-storage/                    # 文件存储目录
│   ├── temp/                        # 临时分片
│   └── README.md
├── logs/                            # 日志文件
├── pom.xml                          # Maven 配置
└── README.md                        # 项目说明
```

## 开发指南

### 环境要求

- JDK 1.8+
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+

### 配置数据库

1. 创建数据库：

```sql
CREATE DATABASE smart_transfer DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 修改 `application.yml` 中的数据库配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/smart_transfer?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
```

### 配置 Redis

确保 Redis 服务已启动，默认配置：

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    database: 0
```

### 启动项目

```bash
# 进入项目目录
cd smart-transfer-server

# 编译项目
mvn clean install

# 启动项目
mvn spring-boot:run
```

或者使用 IDE（IntelliJ IDEA/Eclipse）直接运行 `SmartTransferServerApplication` 主类。

### 访问接口

- 后端服务：http://localhost:8081
- 健康检查：http://localhost:8081/actuator/health
- 监控指标：http://localhost:8081/actuator/metrics

## 配置说明

### 文件存储配置

```yaml
transfer:
  storage-path: ./file-storage      # 文件存储路径
  chunk-size: 5242880              # 分片大小：5MB
  temp-path: ./file-storage/temp   # 临时文件路径
```

### TCP 拥塞控制配置

```yaml
congestion-control:
  algorithm: ADAPTIVE              # 算法类型：CUBIC/BBR/ADAPTIVE
  initial-cwnd: 10485760          # 初始拥塞窗口：10MB
  ssthresh: 52428800              # 慢启动阈值：50MB
  max-cwnd: 104857600             # 最大拥塞窗口：100MB
  min-cwnd: 1048576               # 最小拥塞窗口：1MB
  max-rate: 0                     # 最大传输速率（0表示不限制）
  min-rate: 1048576               # 最小传输速率：1MB/s
```

## API 文档

详细 API 文档请参考项目根目录的 `doc/完整项目开发指南.md` 文件。

### 主要接口

- **文件上传**：`POST /api/file/upload/chunk`
- **文件合并**：`POST /api/file/merge`
- **传输任务**：`GET/POST /api/transfer/task`
- **拥塞控制**：`GET/POST /api/congestion/config`
- **系统配置**：`GET/POST /api/system/config`

## 开发规范

请参考项目根目录的 `.cursor/rules/project-development-rules.md` 文件。

### 核心规范

1. **代码注释**：使用 JavaDoc 注释
2. **命名规范**：遵循 Java 命名规范（驼峰命名）
3. **异常处理**：统一异常处理，返回标准 JSON 格式
4. **日志规范**：使用 SLF4J + Logback
5. **事务管理**：使用 `@Transactional` 注解

## 测试

```bash
# 运行所有测试
mvn test

# 运行单个测试类
mvn test -Dtest=YourTestClass

# 跳过测试
mvn clean install -DskipTests
```

## 部署

### 打包

```bash
mvn clean package
```

生成的 JAR 包位于 `target/smart-transfer-server-0.0.1-SNAPSHOT.jar`

### 运行

```bash
java -jar target/smart-transfer-server-0.0.1-SNAPSHOT.jar
```

### 指定配置文件

```bash
java -jar target/smart-transfer-server-0.0.1-SNAPSHOT.jar --spring.config.location=application-prod.yml
```

## 监控

项目集成了 Spring Boot Actuator，可以通过以下端点监控应用状态：

- **健康检查**：`/actuator/health`
- **应用信息**：`/actuator/info`
- **监控指标**：`/actuator/metrics`

## 日志

日志文件位于 `logs/smart-transfer-server.log`

## 许可证

私有项目

## 联系方式

如有问题，请联系开发团队。

