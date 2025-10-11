package com.ruipeng.cloudstorage.controller;

import com.ruipeng.cloudstorage.dto.response.ShareInfoResponse;
import com.ruipeng.cloudstorage.service.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for search operations.
 * Handles file, folder, and share searches.
 */
@RestController
public class SearchController {
    private static final Logger log = LoggerFactory.getLogger(SearchController.class);

    private final SearchService searchService;

    /**
     * Constructor with dependency injection.
     *
     * @param searchService the search service
     */
    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * Searches for active files and folders.
     *
     * @param query the search query
     * @param userId the user ID
     * @return search results
     */
    @GetMapping("/search")
    public ResponseEntity<?> search(
            @RequestParam("query") String query,
            @RequestParam("userId") Long userId) {
        logSearch(query, userId);

        try {
            Map<String, Object> results = searchService.search(query, userId);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("Search failed: query={}, userId={}", query, userId, e);
            return buildErrorResponse("Search failed: " + e.getMessage());
        }
    }

    /**
     * Searches in trash (deleted items).
     *
     * @param query the search query
     * @param userId the user ID
     * @return search results from trash
     */
    @GetMapping("/searchBin")
    public ResponseEntity<?> searchBin(
            @RequestParam("query") String query,
            @RequestParam("userId") Long userId) {
        logSearchBin(query, userId);

        try {
            Map<String, Object> results = searchService.searchBin(query, userId);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("Bin search failed: query={}, userId={}", query, userId, e);
            return buildErrorResponse("Search failed: " + e.getMessage());
        }
    }

    /**
     * Searches user's shares by file name.
     *
     * @param query the search query
     * @param userId the user ID
     * @return list of matching shares
     */
    @GetMapping("/searchShares")
    public ResponseEntity<?> searchShares(
            @RequestParam("query") String query,
            @RequestParam("userId") Long userId) {
        logSearchShares(query, userId);

        try {
            List<ShareInfoResponse> results = searchService.searchShares(query, userId);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("Share search failed: query={}, userId={}", query, userId, e);
            return buildErrorResponse("Search failed: " + e.getMessage());
        }
    }

    // ============ Private Helper Methods ============

    /**
     * Builds error response map.
     */
    private ResponseEntity<?> buildErrorResponse(String errorMessage) {
        Map<String, String> error = new HashMap<>();
        error.put("error", errorMessage);
        return ResponseEntity.badRequest().body(error);
    }

    // ============ Logging Methods ============

    private void logSearch(String query, Long userId) {
        log.info("Searching: query='{}', userId={}", query, userId);
    }

    private void logSearchBin(String query, Long userId) {
        log.info("Searching trash: query='{}', userId={}", query, userId);
    }

    private void logSearchShares(String query, Long userId) {
        log.info("Searching shares: query='{}', userId={}", query, userId);
    }
}
