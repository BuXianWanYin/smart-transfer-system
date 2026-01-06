package com.server.smarttransferserver.common;

/**
 * 统一返回状态码枚举
 * 定义系统中所有API接口的返回状态码
 */
public enum ResultCode {

    /**
     * 成功
     */
    SUCCESS(200, "操作成功"),

    /**
     * 失败
     */
    ERROR(500, "操作失败"),

    /**
     * 参数错误
     */
    PARAM_ERROR(400, "参数错误"),

    /**
     * 未授权
     */
    UNAUTHORIZED(401, "未授权"),

    /**
     * 禁止访问
     */
    FORBIDDEN(403, "禁止访问"),

    /**
     * 资源不存在
     */
    NOT_FOUND(404, "资源不存在"),

    /**
     * 文件不存在
     */
    FILE_NOT_FOUND(4001, "文件不存在"),

    /**
     * 文件已存在
     */
    FILE_ALREADY_EXISTS(4002, "文件已存在"),

    /**
     * 文件上传失败
     */
    FILE_UPLOAD_ERROR(4003, "文件上传失败"),

    /**
     * 文件合并失败
     */
    FILE_MERGE_ERROR(4004, "文件合并失败"),

    /**
     * 文件校验失败
     */
    FILE_CHECK_ERROR(4005, "文件校验失败"),

    /**
     * 分片上传失败
     */
    CHUNK_UPLOAD_ERROR(4006, "分片上传失败"),

    /**
     * 传输任务不存在
     */
    TASK_NOT_FOUND(5001, "传输任务不存在"),

    /**
     * 传输任务创建失败
     */
    TASK_CREATE_ERROR(5002, "传输任务创建失败"),

    /**
     * 传输任务已存在
     */
    TASK_ALREADY_EXISTS(5003, "传输任务已存在"),

    /**
     * 配置不存在
     */
    CONFIG_NOT_FOUND(6001, "配置不存在"),

    /**
     * 配置保存失败
     */
    CONFIG_SAVE_ERROR(6002, "配置保存失败"),

    /**
     * 拥塞控制算法错误
     */
    CONGESTION_ALGORITHM_ERROR(7001, "拥塞控制算法错误"),

    /**
     * 网络监测错误
     */
    NETWORK_MONITOR_ERROR(7002, "网络监测错误");

    /**
     * 状态码
     */
    private final Integer code;

    /**
     * 消息
     */
    private final String message;

    /**
     * 构造方法
     *
     * @param code    状态码
     * @param message 消息
     */
    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 获取状态码
     *
     * @return 状态码
     */
    public Integer getCode() {
        return code;
    }

    /**
     * 获取消息
     *
     * @return 消息
     */
    public String getMessage() {
        return message;
    }
}

