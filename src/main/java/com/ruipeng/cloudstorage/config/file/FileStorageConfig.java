package com.ruipeng.cloudstorage.config.file;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FileStorageConfig {

    @Value("${file.storage.path}")
    private String storagePath;

    public String getStoragePath() {
        return storagePath;
    }
}
