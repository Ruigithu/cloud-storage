package com.ruipeng.cloudstorage.dto.response;


import com.ruipeng.cloudstorage.entity.Folder;
import java.util.List;

/**
 * Builder class for FolderResponse.
 */
public class FolderResponseBuilder {
    private Long rootFolderId;
    private List<Folder> folders;

    public FolderResponseBuilder rootFolderId(Long rootFolderId) {
        this.rootFolderId = rootFolderId;
        return this;
    }

    public FolderResponseBuilder folders(List<Folder> folders) {
        this.folders = folders;
        return this;
    }

    public FolderResponse build() {
        return new FolderResponse(rootFolderId, folders);
    }
}
