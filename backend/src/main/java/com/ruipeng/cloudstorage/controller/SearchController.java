package com.ruipeng.cloudstorage.controller;


import com.ruipeng.cloudstorage.entity.ShareInfoResponse;
import com.ruipeng.cloudstorage.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class SearchController {

    private final SearchService searchService;

    @Autowired
    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(
            @RequestParam("query") String query,
            @RequestParam("userId") Long userId) {

        try {
            Map<String, Object> results = searchService.search(query, userId);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Search failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/searchBin")
    public ResponseEntity<?> searchBin(
            @RequestParam("query") String query,
            @RequestParam("userId") Long userId) {

        try {
            Map<String, Object> results = searchService.searchBin(query, userId);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Search failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/searchShares")
    public ResponseEntity<?> searchShares(
            @RequestParam("query") String query,
            @RequestParam("userId") Long userId) {

        try {
            List<ShareInfoResponse> results = searchService.searchShares(query, userId);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Search failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
