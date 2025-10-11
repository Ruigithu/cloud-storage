package com.ruipeng.cloudstorage.service;

import com.ruipeng.cloudstorage.dto.response.SearchResultResponse;
import com.ruipeng.cloudstorage.dto.response.ShareInfoResponse;
import com.ruipeng.cloudstorage.entity.*;
import com.ruipeng.cloudstorage.mappers.FileMapper;
import com.ruipeng.cloudstorage.mappers.FileVersionMapper;
import com.ruipeng.cloudstorage.mappers.FolderMapper;
import com.ruipeng.cloudstorage.mappers.ShareMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for searching files, folders, and shares.
 *
 * Responsibilities:
 * - Search active files and folders
 * - Search deleted items in trash
 * - Search user's shares
 */
@Service
public class SearchService {
    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    private final FileMapper fileMapper;
    private final FolderMapper folderMapper;
    private final ShareMapper shareMapper;
    private final FileVersionMapper fileVersionMapper;

    @Value("${frontend.url}")
    private String frontendUrl;

    public SearchService(FileMapper fileMapper,
                         FolderMapper folderMapper,
                         ShareMapper shareMapper,
                         FileVersionMapper fileVersionMapper,
                         @Value("${frontend.url}") String frontendUrl) {
        this.fileMapper = fileMapper;
        this.folderMapper = folderMapper;
        this.shareMapper = shareMapper;
        this.fileVersionMapper = fileVersionMapper;
        this.frontendUrl = frontendUrl;
    }

    /**
     * Searches for active files and folders by name.
     *
     * @param query the search query
     * @param userId the user ID
     * @return map containing files and folders
     */
    public Map<String, Object> search(String query, Long userId) {
        validateSearchQuery(query);
        validateUserId(userId);

        log.info("Searching files and folders: query='{}', userId={}", query, userId);

        List<File> files = searchFiles(query, userId);
        List<Folder> folders = searchFolders(query, userId);

        return buildSearchResult(files, folders);
    }

    /**
     * Searches for deleted files and folders in trash.
     *
     * @param query the search query
     * @param userId the user ID
     * @return map containing deleted files and folders
     */
    public Map<String, Object> searchBin(String query, Long userId) {
        validateSearchQuery(query);
        validateUserId(userId);

        log.info("Searching trash: query='{}', userId={}", query, userId);

        List<File> deletedFiles = searchDeletedFiles(query, userId);
        List<Folder> deletedFolders = searchDeletedFolders(query, userId);

        return buildSearchResult(deletedFiles, deletedFolders);
    }

    /**
     * Searches for shares by file name.
     *
     * @param query the search query
     * @param userId the user ID
     * @return list of share information
     */
    public List<ShareInfoResponse> searchShares(String query, Long userId) {
        validateSearchQuery(query);
        validateUserId(userId);

        log.info("Searching shares: query='{}', userId={}", query, userId);

        List<Share> shares = shareMapper.searchSharesByFileName(query, userId);
        return buildShareInfoList(shares, userId);
    }

    /**
     * Searches for files and folders with detailed results.
     *
     * @param query the search query
     * @param userId the user ID
     * @return detailed search results
     */
    public SearchResultResponse searchDetailed(String query, Long userId) {
        validateSearchQuery(query);
        validateUserId(userId);

        log.info("Detailed search: query='{}', userId={}", query, userId);

        Map<String, Object> activeResults = search(query, userId);
        Map<String, Object> trashResults = searchBin(query, userId);
        List<ShareInfoResponse> shareResults = searchShares(query, userId);

        return buildDetailedSearchResult(activeResults, trashResults, shareResults);
    }

    // ============ Private Helper Methods ============

    private void validateSearchQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Search query cannot be null or empty");
        }
    }

    private void validateUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
    }

    private List<File> searchFiles(String query, Long userId) {
        return fileMapper.searchFilesByName(query, userId);
    }

    private List<Folder> searchFolders(String query, Long userId) {
        return folderMapper.searchFoldersByName(query, userId);
    }

    private List<File> searchDeletedFiles(String query, Long userId) {
        return fileMapper.searchDeletedFilesByName(query, userId);
    }

    private List<Folder> searchDeletedFolders(String query, Long userId) {
        return folderMapper.searchDeletedFoldersByName(query, userId);
    }

    private Map<String, Object> buildSearchResult(List<File> files, List<Folder> folders) {
        Map<String, Object> results = new HashMap<>();
        results.put("files", files);
        results.put("folders", folders);
        results.put("totalFiles", files.size());
        results.put("totalFolders", folders.size());
        results.put("totalResults", files.size() + folders.size());
        return results;
    }

    private List<ShareInfoResponse> buildShareInfoList(List<Share> shares, Long userId) {
        List<ShareInfoResponse> responses = new ArrayList<>();

        for (Share share : shares) {
            try {
                ShareInfoResponse response = buildShareInfo(share, userId);
                if (response != null) {
                    responses.add(response);
                }
            } catch (Exception e) {
                log.warn("Failed to build share info: shareId={}", share.getId(), e);
            }
        }

        return responses;
    }

    private ShareInfoResponse buildShareInfo(Share share, Long userId) {
        FileVersion latestVersion = getLatestVersionSafe(share.getFileId());
        if (latestVersion == null) {
            return null;
        }

        File file = getFileSafe(userId, latestVersion.getFileId());
        if (file == null) {
            return null;
        }

        return createShareInfoResponse(share, file, latestVersion);
    }

    private FileVersion getLatestVersionSafe(Long fileId) {
        try {
            return fileVersionMapper.getLatestVersion(fileId);
        } catch (Exception e) {
            log.warn("Failed to get latest version for file: fileId={}", fileId, e);
            return null;
        }
    }

    private File getFileSafe(Long userId, Long fileId) {
        try {
            return fileMapper.getFileByUserIdAndFileId(userId, fileId);
        } catch (Exception e) {
            log.warn("Failed to get file: fileId={}, userId={}", fileId, userId, e);
            return null;
        }
    }

    private ShareInfoResponse createShareInfoResponse(Share share, File file,
                                                      FileVersion version) {
        ShareInfoResponse response = new ShareInfoResponse();
        response.setId(share.getId());
        response.setFileId(version.getFileId());
        response.setFilePath(version.getStoragePath());
        response.setFileName(file.getName());
        response.setShareLink(generateShareLink(share.getId()));
        response.setExpiresAt(share.getExpiresAt());
        response.setType(share.getAccessType());
        response.setActive(share.isActive());
        return response;
    }

    private String generateShareLink(java.util.UUID shareId) {
        return String.format("%s/share/%s", frontendUrl, shareId);
    }

    private SearchResultResponse buildDetailedSearchResult(
            Map<String, Object> activeResults,
            Map<String, Object> trashResults,
            List<ShareInfoResponse> shareResults) {

        SearchResultResponse response = new SearchResultResponse();
        response.setActiveFiles((List<File>) activeResults.get("files"));
        response.setActiveFolders((List<Folder>) activeResults.get("folders"));
        response.setDeletedFiles((List<File>) trashResults.get("files"));
        response.setDeletedFolders((List<Folder>) trashResults.get("folders"));
        response.setShares(shareResults);
        response.setTotalResults(
                (Integer) activeResults.get("totalResults") +
                        (Integer) trashResults.get("totalResults") +
                        shareResults.size()
        );

        return response;
    }
}