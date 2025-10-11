package com.ruipeng.cloudstorage.dto.response;

public class UserInfoResponse {
    private Long userId;
    private String email;
    private String name;

    public UserInfoResponse() {}

    public UserInfoResponse(Long userId, String email) {
        this.userId = userId;
        this.email = email;
    }

    public UserInfoResponse(Long userId, String email, String name) {
        this.userId = userId;
        this.email = email;
        this.name = name;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}