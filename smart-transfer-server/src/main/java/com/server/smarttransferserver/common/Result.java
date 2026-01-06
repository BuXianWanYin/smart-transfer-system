package com.server.smarttransferserver.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一返回结果类
 * 封装API接口的响应数据，包含状态码、消息和数据内容
 *
 * @param <T> 数据类型
 */
@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 状态码
     */
    private Integer code;

    /**
     * 返回消息
     */
    private String message;

    /**
     * 返回数据
     */
    private T data;

    /**
     * 时间戳
     */
    private Long timestamp;

    /**
     * 构造方法
     */
    public Result() {
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 构造方法
     *
     * @param code    状态码
     * @param message 返回消息
     * @param data    返回数据
     */
    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 成功返回（无数据）
     *
     * @param <T> 数据类型
     * @return Result对象
     */
    public static <T> Result<T> success() {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    /**
     * 成功返回（带消息）
     *
     * @param message 返回消息
     * @param <T>     数据类型
     * @return Result对象
     */
    public static <T> Result<T> success(String message) {
        return new Result<>(ResultCode.SUCCESS.getCode(), message, null);
    }

    /**
     * 成功返回（带数据）
     *
     * @param data 返回数据
     * @param <T>  数据类型
     * @return Result对象
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    /**
     * 成功返回（带消息和数据）
     *
     * @param message 返回消息
     * @param data    返回数据
     * @param <T>     数据类型
     * @return Result对象
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), message, data);
    }

    /**
     * 失败返回（无消息）
     *
     * @param <T> 数据类型
     * @return Result对象
     */
    public static <T> Result<T> error() {
        return new Result<>(ResultCode.ERROR.getCode(), ResultCode.ERROR.getMessage(), null);
    }

    /**
     * 失败返回（带消息）
     *
     * @param message 错误消息
     * @param <T>     数据类型
     * @return Result对象
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(ResultCode.ERROR.getCode(), message, null);
    }

    /**
     * 失败返回（带状态码和消息）
     *
     * @param code    状态码
     * @param message 错误消息
     * @param <T>     数据类型
     * @return Result对象
     */
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * 失败返回（带ResultCode枚举）
     *
     * @param resultCode 结果码枚举
     * @param <T>        数据类型
     * @return Result对象
     */
    public static <T> Result<T> error(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    /**
     * 自定义返回
     *
     * @param code    状态码
     * @param message 返回消息
     * @param data    返回数据
     * @param <T>     数据类型
     * @return Result对象
     */
    public static <T> Result<T> build(Integer code, String message, T data) {
        return new Result<>(code, message, data);
    }
}

