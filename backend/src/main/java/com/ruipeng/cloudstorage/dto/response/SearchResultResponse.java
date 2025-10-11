package com.ruipeng.cloudstorage.dto.response;


import com.ruipeng.cloudstorage.entity.File;
import com.ruipeng.cloudstorage.entity.Folder;

import java.util.List;

/**
 * Response DTO for detailed search results.
 */
public class SearchResultResponse {
    private List<File> activeFiles;
    private List<Folder> activeFolders;
    private List<File> deletedFiles;
    private List<Folder> deletedFolders;
    private List<ShareInfoResponse> shares;
    private Integer totalResults;

    public SearchResultResponse() {}

    public List<File> getActiveFiles() {
        return activeFiles;
    }

    public void setActiveFiles(List<File> activeFiles) {
        this.activeFiles = activeFiles;
    }

    public List<Folder> getActiveFolders() {
        return activeFolders;
    }

    public void setActiveFolders(List<Folder> activeFolders) {
        this.activeFolders = activeFolders;
    }

    public List<File> getDeletedFiles() {
        return deletedFiles;
    }

    public void setDeletedFiles(List<File> deletedFiles) {
        this.deletedFiles = deletedFiles;
    }

    public List<Folder> getDeletedFolders() {
        return deletedFolders;
    }

    public void setDeletedFolders(List<Folder> deletedFolders) {
        this.deletedFolders = deletedFolders;
    }

    public List<ShareInfoResponse> getShares() {
        return shares;
    }

    public void setShares(List<ShareInfoResponse> shares) {
        this.shares = shares;
    }

    public Integer getTotalResults() {
        return totalResults;
    }

    public void setTotalResults(Integer totalResults) {
        this.totalResults = totalResults;
    }
}