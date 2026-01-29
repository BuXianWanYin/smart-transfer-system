package com.server.smarttransferserver.congestion;

/**
 * 重置类型枚举
 */
public enum ResetType {
    /** 全量重置（初始化时） */
    FULL_RESET,
    /** 增量重置（切换时） */
    INCREMENTAL_RESET
}
