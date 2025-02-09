package com.ruipeng.cloudstorage.entity;

public class ShareResponse {
    private String shareLink;

    public ShareResponse() {
    }

    public ShareResponse(String shareLink) {
        this.shareLink = shareLink;
    }

    public String getShareLink() {
        return shareLink;
    }

    public void setShareLink(String shareLink) {
        this.shareLink = shareLink;
    }
}

