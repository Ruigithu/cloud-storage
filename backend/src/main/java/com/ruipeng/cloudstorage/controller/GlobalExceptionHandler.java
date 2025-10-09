package com.ruipeng.cloudstorage.controller;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    public GlobalExceptionHandler(){}
    @ExceptionHandler(IllegalArgumentException.class)
    public Map<String,Object> handleIllegalArgumentException(Exception e){
        Map <String,Object> error = new HashMap<>();
        error.put("code",400);
        error.put("message",e.getMessage());
        return error;
    }
}
