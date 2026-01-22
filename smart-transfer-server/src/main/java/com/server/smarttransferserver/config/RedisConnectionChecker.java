package com.server.smarttransferserver.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

/**
 * Redis 连接检查器
 * 在应用启动时检查 Redis 连接，如果连接失败则终止程序
 */
@Component
public class RedisConnectionChecker implements ApplicationRunner {
    
    private static final Logger log = LoggerFactory.getLogger(RedisConnectionChecker.class);
    
    private final RedisConnectionFactory redisConnectionFactory;
    
    public RedisConnectionChecker(RedisConnectionFactory redisConnectionFactory) {
        this.redisConnectionFactory = redisConnectionFactory;
    }
    
    @Override
    public void run(ApplicationArguments args) {
        
        RedisConnection connection = null;
        try {
            // 获取 Redis 连接
            connection = redisConnectionFactory.getConnection();
            
            // 执行 PING 命令测试连接
            String pong = connection.ping();
            
            // 检查 PING 响应（可能返回 "PONG" 或 "pong"）
            if (pong != null && ("PONG".equalsIgnoreCase(pong))) {
            } else {
                log.error("Redis 连接异常：PING 命令返回异常值: {}", pong);
                shutdownApplication("Redis 连接异常");
            }
            
        } catch (Exception e) {
            log.error("Redis 连接失败！", e);
            log.error("错误信息: {}", e.getMessage());
            shutdownApplication("Redis 连接失败: " + e.getMessage());
        } finally {
            // 关闭连接
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception e) {
                    log.warn("关闭 Redis 连接时出错: {}", e.getMessage());
                }
            }
        }
    }
    
    /**
     * 终止应用程序
     * 
     * @param reason 终止原因
     */
    private void shutdownApplication(String reason) {
        log.error("==========================================");
        log.error("应用启动失败：{}", reason);
        log.error("请确保 Redis 服务已启动并可以正常连接");
        log.error("Redis 配置请检查 application.yml 中的 spring.redis 配置项");
        log.error("==========================================");
        
        // 终止 JVM，退出码为 1 表示异常退出
        System.exit(1);
    }
}
