package com.ruipeng.cloudstorage.service;


import com.ruipeng.cloudstorage.entity.*;
import com.ruipeng.cloudstorage.mappers.FileMapper;
import com.ruipeng.cloudstorage.mappers.FileVersionMapper;
import com.ruipeng.cloudstorage.mappers.FolderMapper;
import com.ruipeng.cloudstorage.mappers.ShareMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SearchService {

    private final FileMapper fileMapper;
    private final FolderMapper folderMapper;
    private final ShareMapper shareMapper;
    private final FileVersionMapper fileVersionMapper;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Autowired
    public SearchService(FileMapper fileMapper, FolderMapper folderMapper, ShareMapper shareMapper, FileVersionMapper fileVersionMapper) {
        this.fileMapper = fileMapper;
        this.folderMapper = folderMapper;
        this.shareMapper = shareMapper;
        this.fileVersionMapper = fileVersionMapper;
    }

    public Map<String, Object> search(String query, Long userId) {
        // Search for files and folders that match the query
        List<File> files = fileMapper.searchFilesByName(query, userId);
        List<Folder> folders = folderMapper.searchFoldersByName(query, userId);

        Map<String, Object> results = new HashMap<>();
        results.put("files", files);
        results.put("folders", folders);

        return results;
    }

    public Map<String, Object> searchBin(String query, Long userId) {
        // Search for deleted files and folders that match the query
        List<File> files = fileMapper.searchDeletedFilesByName(query, userId);
        List<Folder> folders = folderMapper.searchDeletedFoldersByName(query, userId);

        Map<String, Object> results = new HashMap<>();
        results.put("files", files);
        results.put("folders", folders);

        return results;
    }

    public List<ShareInfoResponse> searchShares(String query, Long userId) {
        List<Share> shares = shareMapper.searchSharesByFileName(query, userId);
        List<ShareInfoResponse> shareInfoResponses = new ArrayList<>();

        for (Share share : shares) {
            FileVersion latestVersion = fileVersionMapper.getLatestVersion(share.getFileId());
            File file = fileMapper.getFileByUserIdAndFileId(userId, latestVersion.getFileId());

            if (file == null) {
                continue;
            }

            ShareInfoResponse shareInfo = new ShareInfoResponse();
            shareInfo.setId(share.getId());
            shareInfo.setFileId(latestVersion.getFileId());
            shareInfo.setFilePath(latestVersion.getStoragePath());
            shareInfo.setFileName(file.getName());
            shareInfo.setShareLink(String.format("%s/share/%s", frontendUrl, share.getId()));
            shareInfo.setExpiresAt(share.getExpiresAt());
            shareInfo.setType(share.getAccessType());
            shareInfo.setActive(share.isActive());

            shareInfoResponses.add(shareInfo);
        }

        return shareInfoResponses;
    }


}