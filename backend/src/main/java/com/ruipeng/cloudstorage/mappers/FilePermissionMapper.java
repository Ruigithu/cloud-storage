package com.ruipeng.cloudstorage.mappers;

import com.ruipeng.cloudstorage.entity.FilePermission;
import org.apache.ibatis.annotations.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Mapper
public interface FilePermissionMapper {
    @Insert("INSERT INTO file_permissions (file_id, folder_id, user_id, permission, created_at, created_by) " +
            "VALUES (#{fileId}, #{folderId}, #{userId}, " +
            "#{permission,typeHandler=com.ruipeng.cloudstorage.config.mybatis.PermissionTypeHandler}, " +
            "#{createdAt,jdbcType=TIMESTAMP}, #{createdBy})")
    @Options(useGeneratedKeys = true, keyProperty = "id",keyColumn = "id")
    void insert(FilePermission filePermission);


    @Select("SELECT * FROM file_permissions " +
            "WHERE folder_id = #{folderId} AND user_id = #{userId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "fileId", column = "file_id"),
            @Result(property = "folderId", column = "folder_id"),
            @Result(property = "userId", column = "user_id"),
            @Result(property = "permission", column = "permission",
                    typeHandler = com.ruipeng.cloudstorage.config.mybatis.PermissionTypeHandler.class),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "createdBy", column = "created_by")
    })
    FilePermission findByFolderIdAndUserId(@Param("folderId") Long folderId, @Param("userId") Long userId);
    @Select("SELECT * FROM file_permissions " +
            "WHERE folder_id = #{folderId} AND user_id = #{userId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "fileId", column = "file_id"),
            @Result(property = "folderId", column = "folder_id"),
            @Result(property = "userId", column = "user_id"),
            @Result(property = "permission", column = "permission",
                    typeHandler = com.ruipeng.cloudstorage.config.mybatis.PermissionTypeHandler.class),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "createdBy", column = "created_by")
    })
    List<FilePermission> findListByFolderIdAndUserId(@Param("folderId") Long folderId, @Param("userId") Long userId);

    @Select("SELECT * FROM file_permissions " +
            " WHERE file_id = #{fileId} AND user_id = #{userId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "fileId", column = "file_id"),
            @Result(property = "folderId", column = "folder_id"),
            @Result(property = "userId", column = "user_id"),
            @Result(property = "permission", column = "permission",
                    typeHandler = com.ruipeng.cloudstorage.config.mybatis.PermissionTypeHandler.class),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "createdBy", column = "created_by")
    })
    FilePermission getPermission(Long fileId, Long userId);

    @Delete("DELETE FROM file_permissions WHERE file_id = #{fileId}")
    void deleteByFileId(Long fileId);

    @Delete(" DELETE FROM file_permissions WHERE folder_id = #{id}")
    void deleteByFolderId(Long id);

    @Update("UPDATE file_permissions SET " +
            "file_id = #{fileId}, " +
            "permission = #{permission}, " +
            "created_at = #{createdAt}, " +
            "created_by = #{createdBy} " +
            "WHERE folder_id = #{folderId} AND user_id = #{userId}")
    void update(FilePermission permission);

    @Select("SELECT * FROM file_permissions " +
            "WHERE file_id = #{fileId} AND user_id = #{userId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "fileId", column = "file_id"),
            @Result(property = "folderId", column = "folder_id"),
            @Result(property = "userId", column = "user_id"),
            @Result(property = "permission", column = "permission",
                    typeHandler = com.ruipeng.cloudstorage.config.mybatis.PermissionTypeHandler.class),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "createdBy", column = "created_by")
    })
    FilePermission findByFileIdAndUserId(Long fileId, Long userId);
}
