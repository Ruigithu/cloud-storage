package com.ruipeng.cloudstorage.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;


import java.time.Instant;

/**
 * 统一API响应封装类
 * @param <T> 响应数据类型
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private String error;
    private Instant timestamp;

    public ApiResponse(boolean b, Object o, T data, Object o1, Instant now) {
    }

    // 成功响应 - 带数据
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, null, data, null, Instant.now());
    }

    // 成功响应 - 带消息和数据
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, null, Instant.now());
    }

    // 成功响应 - 仅消息
    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(true, message, null, null, Instant.now());
    }

    // 失败响应
    public static <T> ApiResponse<T> error(String error) {
        return new ApiResponse<>(false, null, null, error, Instant.now());
    }

    // 失败响应 - 带消息
    public static <T> ApiResponse<T> error(String message, String error) {
        return new ApiResponse<>(false, message, null, error, Instant.now());
    }

}
