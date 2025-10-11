package com.ruipeng.cloudstorage.dto;

import org.springframework.stereotype.Component;

@Component
public class UserInfo {
    private long userId;
    private String userName;

    public UserInfo() {
    }
    public UserInfo(long userId, String userName) {
        this.userId = userId;
        this.userName = userName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }
}
