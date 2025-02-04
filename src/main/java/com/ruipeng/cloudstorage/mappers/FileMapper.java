package com.ruipeng.cloudstorage.mappers;

import com.ruipeng.cloudstorage.entity.File;
import org.apache.ibatis.annotations.*;

import java.time.Instant;
import java.util.List;

@Mapper
public interface FileMapper {

    @Select("SELECT * FROM files WHERE owner_id = #{ownerId} AND folder_id=#{folderId} AND is_deleted = FALSE")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "name", column = "name"),
            @Result(property = "mimeType", column = "mime_type"),
            @Result(property = "size", column = "size"),
            @Result(property = "ownerId", column = "owner_id"),
            @Result(property = "folderId", column = "folder_id"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "isDeleted", column = "is_deleted")
    })
    List<File> getFilesByUserIdAndFolderId(long ownerId,long folderId);

    @Select("SELECT * FROM files WHERE id = #{fileId} AND is_deleted = FALSE")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "name", column = "name"),
            @Result(property = "mimeType", column = "mime_type"),
            @Result(property = "size", column = "size"),
            @Result(property = "ownerId", column = "owner_id"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "isDeleted", column = "is_deleted")
    })
    File getFileById(long fileId);

    @Select("SELECT * FROM files WHERE id = #{fileId} AND is_deleted = TRUE")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "name", column = "name"),
            @Result(property = "mimeType", column = "mime_type"),
            @Result(property = "size", column = "size"),
            @Result(property = "ownerId", column = "owner_id"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "isDeleted", column = "is_deleted")
    })
    File getDeletedFileById(long fileId);

    @Insert("INSERT INTO files (name, mime_type, size, owner_id, folder_id, created_at, updated_at) " +
            "VALUES (#{name}, #{mimeType}, #{size}, #{ownerId}, #{folderId}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")  // 确保 keyColumn 正确
    long insertFile(File file);


    @Update("UPDATE files SET is_deleted = TRUE WHERE id = #{fileId}")
    int softDeleteById(int fileId);

    @Delete("DELETE FROM files WHERE id = #{fileId}")
    int deleteById(int fileId);

    @Select("SELECT * from files where folder_id = #{folderId}")
    List<File> folderExists(long folderId);

    @Delete("DELETE FROM files WHERE id = #{fileId}")
    int deleteFile(Long fileId);

    @Select({
            "<script>",
            "SELECT * FROM files WHERE",
            "<choose>",
            "<when test='collect != null and collect.size() > 0'>",  // 如果 collect 列表不为空
            "folder_id IN",
            "<foreach collection='collect' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "OR folder_id = #{folderId}",  // 同时查询当前文件夹的文件
            "</when>",
            "<otherwise>",  // 如果 collect 列表为空
            "folder_id = #{folderId}",  // 只查询当前文件夹的文件
            "</otherwise>",
            "</choose>",
            "</script>"})
    List<File> findFilesByFolderIds(List<Long> collect, Long folderId);
    @Select("SELECT * FROM files WHERE folder_id = #{id} AND is_deleted = FALSE")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "name", column = "name"),
            @Result(property = "mimeType", column = "mime_type"),
            @Result(property = "size", column = "size"),
            @Result(property = "ownerId", column = "owner_id"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "isDeleted", column = "is_deleted")
    })
    List<File> getFilesByFolderId(Long id);

    @Update("UPDATE files " +
            "SET " +
            "    name = #{name}, " +
            "    folder_id = #{folderId}, " +
            "    owner_id = #{ownerId}, " +
            "    mime_type = #{mimeType}, " +
            "    size = #{size}, " +
            "    updated_at = #{updatedAt}, " +
            "    is_deleted = #{isDeleted} " +
            "WHERE id = #{id}")
    int updateFile(File file);

    @Select("SELECT * FROM files WHERE is_deleted = true AND updated_at < #{date}")
    List<File> getSoftDeletedFilesBefore(Instant date);


        @Select("SELECT * FROM files WHERE owner_id = #{ownerId} AND folder_id = #{folderId} AND is_deleted = true")
        @Results({
                @Result(property = "id", column = "id"),
                @Result(property = "name", column = "name"),
                @Result(property = "mimeType", column = "mime_type"),
                @Result(property = "size", column = "size"),
                @Result(property = "ownerId", column = "owner_id"),
                @Result(property = "folderId", column = "folder_id"),
                @Result(property = "createdAt", column = "created_at"),
                @Result(property = "updatedAt", column = "updated_at"),
                @Result(property = "isDeleted", column = "is_deleted")
        })
        List<File> getDeletedFilesByUserIdAndFolderId(@Param("ownerId") long ownerId, @Param("folderId") long folderId);

        @Update("UPDATE files " +
            "SET " +
            "    updated_at = #{updatedAt}, " +
            "    is_deleted = #{isDeleted} " +
            "WHERE id = #{id}")
    int updateFileDeleteStatus(File file);

    @Select("SELECT * FROM files WHERE owner_id = #{ownerId}  AND is_deleted = true")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "name", column = "name"),
            @Result(property = "mimeType", column = "mime_type"),
            @Result(property = "size", column = "size"),
            @Result(property = "ownerId", column = "owner_id"),
            @Result(property = "folderId", column = "folder_id"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "isDeleted", column = "is_deleted")
    })
    File getDeletedFileByUserId( Long ownerId);
}

