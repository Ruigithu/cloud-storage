package com.ruipeng.cloudstorage.service;

import com.ruipeng.cloudstorage.entity.File;
import com.ruipeng.cloudstorage.mappers.FileMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FileService {
   private FileMapper fileMapper;

    public FileService(FileMapper fileMapper) {
        this.fileMapper = fileMapper;
    }

    public int uploadFile(File file){
        return fileMapper.insert(file);
    }

    public List<File> getFiles(int userid){
        return fileMapper.getFilesByUserId(userid);
    }

    public int deleteById(int fileid){
        return fileMapper.deleteByFileid(fileid);
    }

}
