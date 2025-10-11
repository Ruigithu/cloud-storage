package com.ruipeng.cloudstorage.exception;

/**
 * 权限不足异常
 */
public class InsufficientPermissionException extends RuntimeException {
    public InsufficientPermissionException(String message) {
        super(message);
    }
}
