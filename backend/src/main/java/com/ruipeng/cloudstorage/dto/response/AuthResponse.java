package com.ruipeng.cloudstorage.dto.response;


public class AuthResponse {
    private boolean success;
    private String token;
    private String email;
    private String message;


    public AuthResponse(boolean success, String token, String email) {
        this.success = success;
        this.token = token;
        this.email = email;
    }


    public AuthResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }


    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
