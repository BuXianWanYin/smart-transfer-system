package com.server.smarttransferserver.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 当客户端已断开连接（如取消上传）时，框架在写回响应会抛出 ClientAbortException。
 * 本解析器捕获该异常并静默处理，避免刷屏式错误日志。
 */
@Slf4j
public class ClientAbortExceptionResolver implements HandlerExceptionResolver, Ordered {

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    @Nullable
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response,
                                         @Nullable Object handler, Exception ex) {
        if (isClientAbort(ex)) {
            log.debug("客户端已断开连接，静默处理: {}", ex.getMessage());
            return new ModelAndView();
        }
        return null;
    }

    private static boolean isClientAbort(Throwable t) {
        while (t != null) {
            String name = t.getClass().getName();
            if (name.contains("ClientAbortException")) {
                return true;
            }
            if (t.getMessage() != null && (
                t.getMessage().contains("远程主机强迫关闭") ||
                t.getMessage().contains("Connection reset") ||
                t.getMessage().contains("Broken pipe"))) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }
}
