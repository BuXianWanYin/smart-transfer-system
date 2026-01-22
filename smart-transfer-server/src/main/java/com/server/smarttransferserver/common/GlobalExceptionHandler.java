package com.server.smarttransferserver.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MultipartException;

import java.io.EOFException;
import java.io.IOException;

/**
 * 全局异常处理器
 * 统一处理应用中的异常，返回友好的错误信息
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理文件上传时的连接中断异常
     * 当客户端取消上传时，连接会被关闭，导致EOFException或MultipartException
     * 这种情况不需要记录错误日志，只需要静默处理
     *
     * @param e 异常对象
     * @return 错误响应
     */
    @ExceptionHandler({EOFException.class, MultipartException.class})
    public Result<String> handleUploadCancelException(Exception e) {
        // 检查是否是客户端取消上传导致的异常
        if (e instanceof EOFException || 
            (e instanceof MultipartException && 
             e.getCause() instanceof IOException && 
             e.getCause().getCause() instanceof EOFException)) {
            // 客户端取消上传，静默处理，不记录错误日志
            log.debug("客户端取消上传，连接已关闭");
            return Result.error("上传已取消");
        }
        
        // 其他multipart异常，记录警告日志
        log.warn("文件上传异常: {}", e.getMessage());
        return Result.error("文件上传失败: " + e.getMessage());
    }

    /**
     * 处理IO异常
     * 包括连接中断等情况
     *
     * @param e IO异常
     * @return 错误响应
     */
    @ExceptionHandler(IOException.class)
    public Result<String> handleIOException(IOException e) {
        // 检查是否是EOF异常（客户端取消）
        if (e instanceof EOFException || e.getCause() instanceof EOFException) {
            log.debug("客户端连接已关闭");
            return Result.error("连接已关闭");
        }
        
        log.error("IO异常: {}", e.getMessage(), e);
        return Result.error("IO操作失败: " + e.getMessage());
    }

    /**
     * 处理所有未捕获的异常
     *
     * @param e 异常对象
     * @return 错误响应
     */
    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e) {
        log.error("系统异常: {}", e.getMessage(), e);
        return Result.error("系统异常: " + e.getMessage());
    }
}
