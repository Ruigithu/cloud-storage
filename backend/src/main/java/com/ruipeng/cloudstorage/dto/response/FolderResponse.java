package com.ruipeng.cloudstorage.dto.response;


import com.ruipeng.cloudstorage.entity.Folder;
import java.util.List;

public class FolderResponse {
    private Long rootFolderId;
    private List<Folder> folders;

    public FolderResponse() {}

    public FolderResponse(Long rootFolderId, List<Folder> folders) {
        this.rootFolderId = rootFolderId;
        this.folders = folders;
    }

    public static FolderResponseBuilder builder() {
        return new FolderResponseBuilder();
    }

    public Long getRootFolderId() { return rootFolderId; }
    public void setRootFolderId(Long rootFolderId) { this.rootFolderId = rootFolderId; }

    public List<Folder> getFolders() { return folders; }
    public void setFolders(List<Folder> folders) { this.folders = folders; }
}
