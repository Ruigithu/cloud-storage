package com.ruipeng.cloudstorage.mappers;

import com.ruipeng.cloudstorage.entity.File;
import com.ruipeng.cloudstorage.entity.FileVersion;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface FileVersionMapper {

    @Select("SELECT  version_number FROM file_versions WHERE file_id = #{fileId}")
    @Results({
            @Result(property = "versionNumber", column = "version_number")
    })
    int getVersionNumber(long fileId);

    @Insert("INSERT INTO file_versions ( file_id, version_number, storage_path, size, created_by,created_at,comment) " +
            "VALUES ( #{fileId}, #{versionNumber}, #{storagePath},#{size}, #{createdBy}, CURRENT_TIMESTAMP,#{comment})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertVersion(FileVersion fileVersion);

    @Select("SELECT * FROM file_versions WHERE file_id = #{fileId}")
    @Results({
            @Result(property = "id",column = "id"),
            @Result(property = "fileId",column = "file_id"),
            @Result(property = "versionNumber",column = "version_number"),
            @Result(property = "storagePath",column = "storage_path"),
            @Result(property = "size",column = "size"),
            @Result(property = "createdBy",column = "created_by"),
            @Result(property = "createdAt",column = "created_at"),
            @Result(property = "comment",column = "comment")
    })
    List<FileVersion> getVersionsByFileId(Long fileId);

    @Delete("DELETE FROM file_versions WHERE file_id = #{fileId}")
    void deleteByFileId(Long fileId);

    @Select("SELECT * FROM file_versions WHERE file_id = #{fileId} " +
            "ORDER BY version_number DESC LIMIT 1")
    @Results({
            @Result(property = "fileId", column = "file_id"),
            @Result(property = "versionNumber", column = "version_number"),
            @Result(property = "storagePath", column = "storage_path"),
            @Result(property = "createdBy", column = "created_by")
    })
    FileVersion getLatestVersion(@Param("fileId") Long fileId);
}
