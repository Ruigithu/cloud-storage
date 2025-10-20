package com.ruipeng.cloudstorage.service;

import com.ruipeng.cloudstorage.dto.response.SearchResultResponse;
import com.ruipeng.cloudstorage.dto.response.ShareInfoResponse;
import com.ruipeng.cloudstorage.entity.*;
import com.ruipeng.cloudstorage.mappers.FileMapper;
import com.ruipeng.cloudstorage.mappers.FileVersionMapper;
import com.ruipeng.cloudstorage.mappers.FolderMapper;
import com.ruipeng.cloudstorage.mappers.ShareMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SearchService Unit Test")
class SearchServiceTest {

    @Mock
    private FileMapper fileMapper;

    @Mock
    private FolderMapper folderMapper;

    @Mock
    private ShareMapper shareMapper;

    @Mock
    private FileVersionMapper fileVersionMapper;

    private SearchService searchService;

    private static final String FRONTEND_URL = "https://example.com";
    private static final Long TEST_USER_ID = 1L;
    private static final String TEST_QUERY = "test";

    @BeforeEach
    void setUp() {
        searchService = new SearchService(
                fileMapper,
                folderMapper,
                shareMapper,
                fileVersionMapper,
                FRONTEND_URL
        );
    }

    // ============ search Tests ============

    @Test
    @DisplayName("search returns files and folders successfully")
    void search_success() {
        String query = "document";
        Long userId = TEST_USER_ID;

        List<File> mockFiles = Arrays.asList(
                createMockFile(1L, "document1.pdf"),
                createMockFile(2L, "document2.pdf")
        );
        List<Folder> mockFolders = Arrays.asList(
                createMockFolder(10L, "Documents"),
                createMockFolder(11L, "My Documents")
        );

        when(fileMapper.searchFilesByName(query, userId)).thenReturn(mockFiles);
        when(folderMapper.searchFoldersByName(query, userId)).thenReturn(mockFolders);

        Map<String, Object> result = searchService.search(query, userId);

        assertThat(result).isNotNull();
        assertThat(result).containsKeys("files", "folders", "totalFiles", "totalFolders", "totalResults");
        assertThat((List<File>) result.get("files")).hasSize(2);
        assertThat((List<Folder>) result.get("folders")).hasSize(2);
        assertThat(result.get("totalFiles")).isEqualTo(2);
        assertThat(result.get("totalFolders")).isEqualTo(2);
        assertThat(result.get("totalResults")).isEqualTo(4);

        verify(fileMapper).searchFilesByName(query, userId);
        verify(folderMapper).searchFoldersByName(query, userId);
    }

    @Test
    @DisplayName("search returns empty results when no matches found")
    void search_noMatches_returnsEmpty() {
        String query = "nonexistent";
        Long userId = TEST_USER_ID;

        when(fileMapper.searchFilesByName(query, userId)).thenReturn(Collections.emptyList());
        when(folderMapper.searchFoldersByName(query, userId)).thenReturn(Collections.emptyList());

        Map<String, Object> result = searchService.search(query, userId);

        assertThat(result).isNotNull();
        assertThat((List<File>) result.get("files")).isEmpty();
        assertThat((List<Folder>) result.get("folders")).isEmpty();
        assertThat(result.get("totalResults")).isEqualTo(0);

        verify(fileMapper).searchFilesByName(query, userId);
        verify(folderMapper).searchFoldersByName(query, userId);
    }

    @Test
    @DisplayName("search throws exception when query is null")
    void search_queryNull_throwsException() {
        Long userId = TEST_USER_ID;

        assertThatThrownBy(() -> searchService.search(null, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Search query cannot be null or empty");

        verify(fileMapper, never()).searchFilesByName(anyString(), anyLong());
        verify(folderMapper, never()).searchFoldersByName(anyString(), anyLong());
    }

    @Test
    @DisplayName("search throws exception when query is empty")
    void search_queryEmpty_throwsException() {
        Long userId = TEST_USER_ID;

        assertThatThrownBy(() -> searchService.search("", userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Search query cannot be null or empty");

        verify(fileMapper, never()).searchFilesByName(anyString(), anyLong());
        verify(folderMapper, never()).searchFoldersByName(anyString(), anyLong());
    }

    @Test
    @DisplayName("search throws exception when query is whitespace only")
    void search_queryWhitespace_throwsException() {
        Long userId = TEST_USER_ID;

        assertThatThrownBy(() -> searchService.search("   ", userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Search query cannot be null or empty");

        verify(fileMapper, never()).searchFilesByName(anyString(), anyLong());
        verify(folderMapper, never()).searchFoldersByName(anyString(), anyLong());
    }

    @Test
    @DisplayName("search throws exception when user id is null")
    void search_userIdNull_throwsException() {
        String query = "test";

        assertThatThrownBy(() -> searchService.search(query, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User ID cannot be null");

        verify(fileMapper, never()).searchFilesByName(anyString(), anyLong());
        verify(folderMapper, never()).searchFoldersByName(anyString(), anyLong());
    }

    // ============ searchBin Tests ============

    @Test
    @DisplayName("search bin returns deleted files and folders successfully")
    void searchBin_success() {
        String query = "deleted";
        Long userId = TEST_USER_ID;

        List<File> mockDeletedFiles = Arrays.asList(
                createMockFile(1L, "deleted1.pdf"),
                createMockFile(2L, "deleted2.pdf")
        );
        List<Folder> mockDeletedFolders = Arrays.asList(
                createMockFolder(10L, "Deleted Folder")
        );

        when(fileMapper.searchDeletedFilesByName(query, userId)).thenReturn(mockDeletedFiles);
        when(folderMapper.searchDeletedFoldersByName(query, userId)).thenReturn(mockDeletedFolders);

        Map<String, Object> result = searchService.searchBin(query, userId);

        assertThat(result).isNotNull();
        assertThat((List<File>) result.get("files")).hasSize(2);
        assertThat((List<Folder>) result.get("folders")).hasSize(1);
        assertThat(result.get("totalFiles")).isEqualTo(2);
        assertThat(result.get("totalFolders")).isEqualTo(1);
        assertThat(result.get("totalResults")).isEqualTo(3);

        verify(fileMapper).searchDeletedFilesByName(query, userId);
        verify(folderMapper).searchDeletedFoldersByName(query, userId);
    }

    @Test
    @DisplayName("search bin returns empty results when no deleted items match")
    void searchBin_noMatches_returnsEmpty() {
        String query = "nonexistent";
        Long userId = TEST_USER_ID;

        when(fileMapper.searchDeletedFilesByName(query, userId)).thenReturn(Collections.emptyList());
        when(folderMapper.searchDeletedFoldersByName(query, userId)).thenReturn(Collections.emptyList());

        Map<String, Object> result = searchService.searchBin(query, userId);

        assertThat(result).isNotNull();
        assertThat((List<File>) result.get("files")).isEmpty();
        assertThat((List<Folder>) result.get("folders")).isEmpty();
        assertThat(result.get("totalResults")).isEqualTo(0);

        verify(fileMapper).searchDeletedFilesByName(query, userId);
        verify(folderMapper).searchDeletedFoldersByName(query, userId);
    }

    @Test
    @DisplayName("search bin throws exception when query is null")
    void searchBin_queryNull_throwsException() {
        Long userId = TEST_USER_ID;

        assertThatThrownBy(() -> searchService.searchBin(null, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Search query cannot be null or empty");

        verify(fileMapper, never()).searchDeletedFilesByName(anyString(), anyLong());
        verify(folderMapper, never()).searchDeletedFoldersByName(anyString(), anyLong());
    }

    @Test
    @DisplayName("search bin throws exception when user id is null")
    void searchBin_userIdNull_throwsException() {
        String query = "test";

        assertThatThrownBy(() -> searchService.searchBin(query, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User ID cannot be null");

        verify(fileMapper, never()).searchDeletedFilesByName(anyString(), anyLong());
        verify(folderMapper, never()).searchDeletedFoldersByName(anyString(), anyLong());
    }

    // ============ searchShares Tests ============

    @Test
    @DisplayName("search shares returns share info list successfully")
    void searchShares_success() {
        String query = "shared";
        Long userId = TEST_USER_ID;
        UUID shareId = UUID.randomUUID();

        Share mockShare = createMockShare(shareId, 1L, PermissionType.READ);
        FileVersion mockVersion = createMockFileVersion(1L, "path/to/file.pdf");
        File mockFile = createMockFile(1L, "shared-document.pdf");

        when(shareMapper.searchSharesByFileName(query, userId))
                .thenReturn(Arrays.asList(mockShare));
        when(fileVersionMapper.getLatestVersion(1L)).thenReturn(mockVersion);
        when(fileMapper.getFileByUserIdAndFileId(userId, 1L)).thenReturn(mockFile);

        List<ShareInfoResponse> result = searchService.searchShares(query, userId);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFileId()).isEqualTo(1L);
        assertThat(result.get(0).getFileName()).isEqualTo("shared-document.pdf");
        assertThat(result.get(0).getShareLink()).contains(shareId.toString());
        assertThat(result.get(0).getType()).isEqualTo(PermissionType.READ);

        verify(shareMapper).searchSharesByFileName(query, userId);
        verify(fileVersionMapper).getLatestVersion(1L);
        verify(fileMapper).getFileByUserIdAndFileId(userId, 1L);
    }

    @Test
    @DisplayName("search shares returns empty list when no shares found")
    void searchShares_noMatches_returnsEmpty() {
        String query = "nonexistent";
        Long userId = TEST_USER_ID;

        when(shareMapper.searchSharesByFileName(query, userId))
                .thenReturn(Collections.emptyList());

        List<ShareInfoResponse> result = searchService.searchShares(query, userId);

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(shareMapper).searchSharesByFileName(query, userId);
        verify(fileVersionMapper, never()).getLatestVersion(anyLong());
    }

    @Test
    @DisplayName("search shares skips share when file version not found")
    void searchShares_versionNotFound_skipsShare() {
        String query = "shared";
        Long userId = TEST_USER_ID;
        UUID shareId = UUID.randomUUID();

        Share mockShare = createMockShare(shareId, 1L, PermissionType.READ);

        when(shareMapper.searchSharesByFileName(query, userId))
                .thenReturn(Arrays.asList(mockShare));
        when(fileVersionMapper.getLatestVersion(1L)).thenReturn(null);

        List<ShareInfoResponse> result = searchService.searchShares(query, userId);

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(shareMapper).searchSharesByFileName(query, userId);
        verify(fileVersionMapper).getLatestVersion(1L);
        verify(fileMapper, never()).getFileByUserIdAndFileId(anyLong(), anyLong());
    }

    @Test
    @DisplayName("search shares skips share when file not found")
    void searchShares_fileNotFound_skipsShare() {
        String query = "shared";
        Long userId = TEST_USER_ID;
        UUID shareId = UUID.randomUUID();

        Share mockShare = createMockShare(shareId, 1L, PermissionType.READ);
        FileVersion mockVersion = createMockFileVersion(1L, "path/to/file.pdf");

        when(shareMapper.searchSharesByFileName(query, userId))
                .thenReturn(Arrays.asList(mockShare));
        when(fileVersionMapper.getLatestVersion(1L)).thenReturn(mockVersion);
        when(fileMapper.getFileByUserIdAndFileId(userId, 1L)).thenReturn(null);

        List<ShareInfoResponse> result = searchService.searchShares(query, userId);

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(shareMapper).searchSharesByFileName(query, userId);
        verify(fileVersionMapper).getLatestVersion(1L);
        verify(fileMapper).getFileByUserIdAndFileId(userId, 1L);
    }

    @Test
    @DisplayName("search shares handles exception gracefully")
    void searchShares_exceptionOccurs_handlesGracefully() {
        String query = "shared";
        Long userId = TEST_USER_ID;
        UUID shareId1 = UUID.randomUUID();
        UUID shareId2 = UUID.randomUUID();

        Share mockShare1 = createMockShare(shareId1, 1L, PermissionType.READ);
        Share mockShare2 = createMockShare(shareId2, 2L, PermissionType.WRITE);
        FileVersion mockVersion = createMockFileVersion(2L, "path/to/file2.pdf");
        File mockFile = createMockFile(2L, "file2.pdf");

        when(shareMapper.searchSharesByFileName(query, userId))
                .thenReturn(Arrays.asList(mockShare1, mockShare2));
        when(fileVersionMapper.getLatestVersion(1L))
                .thenThrow(new RuntimeException("Database error"));
        when(fileVersionMapper.getLatestVersion(2L)).thenReturn(mockVersion);
        when(fileMapper.getFileByUserIdAndFileId(userId, 2L)).thenReturn(mockFile);

        List<ShareInfoResponse> result = searchService.searchShares(query, userId);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFileId()).isEqualTo(2L);

        verify(shareMapper).searchSharesByFileName(query, userId);
        verify(fileVersionMapper).getLatestVersion(1L);
        verify(fileVersionMapper).getLatestVersion(2L);
    }

    @Test
    @DisplayName("search shares throws exception when query is null")
    void searchShares_queryNull_throwsException() {
        Long userId = TEST_USER_ID;

        assertThatThrownBy(() -> searchService.searchShares(null, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Search query cannot be null or empty");

        verify(shareMapper, never()).searchSharesByFileName(anyString(), anyLong());
    }

    @Test
    @DisplayName("search shares throws exception when user id is null")
    void searchShares_userIdNull_throwsException() {
        String query = "test";

        assertThatThrownBy(() -> searchService.searchShares(query, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User ID cannot be null");

        verify(shareMapper, never()).searchSharesByFileName(anyString(), anyLong());
    }

    @Test
    @DisplayName("search shares verifies share link format")
    void searchShares_verifyShareLinkFormat() {
        String query = "shared";
        Long userId = TEST_USER_ID;
        UUID shareId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

        Share mockShare = createMockShare(shareId, 1L, PermissionType.READ);
        FileVersion mockVersion = createMockFileVersion(1L, "path/to/file.pdf");
        File mockFile = createMockFile(1L, "document.pdf");

        when(shareMapper.searchSharesByFileName(query, userId))
                .thenReturn(Arrays.asList(mockShare));
        when(fileVersionMapper.getLatestVersion(1L)).thenReturn(mockVersion);
        when(fileMapper.getFileByUserIdAndFileId(userId, 1L)).thenReturn(mockFile);

        List<ShareInfoResponse> result = searchService.searchShares(query, userId);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getShareLink())
                .isEqualTo(FRONTEND_URL + "/share/" + shareId);

        verify(shareMapper).searchSharesByFileName(query, userId);
    }

    // ============ searchDetailed Tests ============

    @Test
    @DisplayName("search detailed returns comprehensive search results")
    void searchDetailed_success() {
        String query = "test";
        Long userId = TEST_USER_ID;

        List<File> activeFiles = Arrays.asList(createMockFile(1L, "test1.pdf"));
        List<Folder> activeFolders = Arrays.asList(createMockFolder(10L, "Test Folder"));
        List<File> deletedFiles = Arrays.asList(createMockFile(2L, "test2.pdf"));
        List<Folder> deletedFolders = Arrays.asList(createMockFolder(11L, "Deleted Test"));

        UUID shareId = UUID.randomUUID();
        Share mockShare = createMockShare(shareId, 3L, PermissionType.READ);
        FileVersion mockVersion = createMockFileVersion(3L, "path/to/file.pdf");
        File mockSharedFile = createMockFile(3L, "test-shared.pdf");

        when(fileMapper.searchFilesByName(query, userId)).thenReturn(activeFiles);
        when(folderMapper.searchFoldersByName(query, userId)).thenReturn(activeFolders);
        when(fileMapper.searchDeletedFilesByName(query, userId)).thenReturn(deletedFiles);
        when(folderMapper.searchDeletedFoldersByName(query, userId)).thenReturn(deletedFolders);
        when(shareMapper.searchSharesByFileName(query, userId))
                .thenReturn(Arrays.asList(mockShare));
        when(fileVersionMapper.getLatestVersion(3L)).thenReturn(mockVersion);
        when(fileMapper.getFileByUserIdAndFileId(userId, 3L)).thenReturn(mockSharedFile);

        SearchResultResponse result = searchService.searchDetailed(query, userId);

        assertThat(result).isNotNull();
        assertThat(result.getActiveFiles()).hasSize(1);
        assertThat(result.getActiveFolders()).hasSize(1);
        assertThat(result.getDeletedFiles()).hasSize(1);
        assertThat(result.getDeletedFolders()).hasSize(1);
        assertThat(result.getShares()).hasSize(1);
        assertThat(result.getTotalResults()).isEqualTo(5);

        verify(fileMapper).searchFilesByName(query, userId);
        verify(folderMapper).searchFoldersByName(query, userId);
        verify(fileMapper).searchDeletedFilesByName(query, userId);
        verify(folderMapper).searchDeletedFoldersByName(query, userId);
        verify(shareMapper).searchSharesByFileName(query, userId);
    }

    @Test
    @DisplayName("search detailed returns empty results when nothing found")
    void searchDetailed_noMatches_returnsEmpty() {
        String query = "nonexistent";
        Long userId = TEST_USER_ID;

        when(fileMapper.searchFilesByName(query, userId)).thenReturn(Collections.emptyList());
        when(folderMapper.searchFoldersByName(query, userId)).thenReturn(Collections.emptyList());
        when(fileMapper.searchDeletedFilesByName(query, userId)).thenReturn(Collections.emptyList());
        when(folderMapper.searchDeletedFoldersByName(query, userId)).thenReturn(Collections.emptyList());
        when(shareMapper.searchSharesByFileName(query, userId)).thenReturn(Collections.emptyList());

        SearchResultResponse result = searchService.searchDetailed(query, userId);

        assertThat(result).isNotNull();
        assertThat(result.getActiveFiles()).isEmpty();
        assertThat(result.getActiveFolders()).isEmpty();
        assertThat(result.getDeletedFiles()).isEmpty();
        assertThat(result.getDeletedFolders()).isEmpty();
        assertThat(result.getShares()).isEmpty();
        assertThat(result.getTotalResults()).isEqualTo(0);

        verify(fileMapper).searchFilesByName(query, userId);
        verify(folderMapper).searchFoldersByName(query, userId);
        verify(fileMapper).searchDeletedFilesByName(query, userId);
        verify(folderMapper).searchDeletedFoldersByName(query, userId);
        verify(shareMapper).searchSharesByFileName(query, userId);
    }

    @Test
    @DisplayName("search detailed throws exception when query is null")
    void searchDetailed_queryNull_throwsException() {
        Long userId = TEST_USER_ID;

        assertThatThrownBy(() -> searchService.searchDetailed(null, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Search query cannot be null or empty");

        verify(fileMapper, never()).searchFilesByName(anyString(), anyLong());
    }

    @Test
    @DisplayName("search detailed throws exception when user id is null")
    void searchDetailed_userIdNull_throwsException() {
        String query = "test";

        assertThatThrownBy(() -> searchService.searchDetailed(query, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User ID cannot be null");

        verify(fileMapper, never()).searchFilesByName(anyString(), anyLong());
    }

    @Test
    @DisplayName("search detailed calculates total results correctly")
    void searchDetailed_calculatesTotalResults() {
        String query = "test";
        Long userId = TEST_USER_ID;

        List<File> activeFiles = Arrays.asList(
                createMockFile(1L, "test1.pdf"),
                createMockFile(2L, "test2.pdf")
        );
        List<Folder> activeFolders = Arrays.asList(createMockFolder(10L, "Test"));
        List<File> deletedFiles = Arrays.asList(createMockFile(3L, "test3.pdf"));
        List<Folder> deletedFolders = Collections.emptyList();

        UUID shareId1 = UUID.randomUUID();
        UUID shareId2 = UUID.randomUUID();
        Share share1 = createMockShare(shareId1, 4L, PermissionType.READ);
        Share share2 = createMockShare(shareId2, 5L, PermissionType.WRITE);

        when(fileMapper.searchFilesByName(query, userId)).thenReturn(activeFiles);
        when(folderMapper.searchFoldersByName(query, userId)).thenReturn(activeFolders);
        when(fileMapper.searchDeletedFilesByName(query, userId)).thenReturn(deletedFiles);
        when(folderMapper.searchDeletedFoldersByName(query, userId)).thenReturn(deletedFolders);
        when(shareMapper.searchSharesByFileName(query, userId))
                .thenReturn(Arrays.asList(share1, share2));

        // Mock for both shares
        when(fileVersionMapper.getLatestVersion(4L))
                .thenReturn(createMockFileVersion(4L, "path4.pdf"));
        when(fileVersionMapper.getLatestVersion(5L))
                .thenReturn(createMockFileVersion(5L, "path5.pdf"));
        when(fileMapper.getFileByUserIdAndFileId(userId, 4L))
                .thenReturn(createMockFile(4L, "file4.pdf"));
        when(fileMapper.getFileByUserIdAndFileId(userId, 5L))
                .thenReturn(createMockFile(5L, "file5.pdf"));

        SearchResultResponse result = searchService.searchDetailed(query, userId);

        assertThat(result).isNotNull();
        // 2 active files + 1 active folder + 1 deleted file + 0 deleted folders + 2 shares = 6
        assertThat(result.getTotalResults()).isEqualTo(6);
    }

    // ============ Helper Methods ============

    private File createMockFile(Long id, String name) {
        File file = new File();
        file.setId(id);
        file.setName(name);
        file.setMimeType("application/pdf");
        file.setSize(1000L);
        file.setOwnerId(TEST_USER_ID);
        file.setFolderId(10L);
        file.setDeleted(false);
        file.setCreatedAt(Instant.now());
        file.setUpdatedAt(Instant.now());
        return file;
    }

    private Folder createMockFolder(Long id, String name) {
        Folder folder = new Folder();
        folder.setId(id);
        folder.setName(name);
        folder.setOwnerId(TEST_USER_ID);
        folder.setParentId(null);
        folder.setDeleted(false);
        folder.setCreatedAt(Instant.now());
        folder.setUpdatedAt(Instant.now());
        return folder;
    }

    private Share createMockShare(UUID id, Long fileId, PermissionType accessType) {
        Share share = new Share();
        share.setId(id);
        share.setFileId(fileId);
        share.setAccessType(accessType);
        share.setActive(true);
        share.setCreatedAt(OffsetDateTime.now());
        share.setExpiresAt(OffsetDateTime.now().plusSeconds(86400));
        return share;
    }

    private FileVersion createMockFileVersion(Long fileId, String storagePath) {
        FileVersion version = new FileVersion();
        version.setId(1L);
        version.setFileId(fileId);
        version.setVersionNumber(1);
        version.setStoragePath(storagePath);
        version.setSize(1000L);
        version.setCreatedAt(Instant.now());
        version.setCreatedBy(TEST_USER_ID);
        return version;
    }
}