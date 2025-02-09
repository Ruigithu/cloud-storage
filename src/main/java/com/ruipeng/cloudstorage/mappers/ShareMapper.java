package com.ruipeng.cloudstorage.mappers;

import com.ruipeng.cloudstorage.entity.Share;
import org.apache.ibatis.annotations.*;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Mapper
public interface ShareMapper {
    @Insert(" INSERT INTO shares " +
            "(id, file_id, created_by, access_type, expires_at, created_at, is_active) " +
            "VALUES (" +
            "#{id,jdbcType=OTHER, typeHandler=com.ruipeng.cloudstorage.config.mybatis.UUIDTypeHandler}, " +
            "#{fileId}, #{createdBy}, " +
            "#{accessType,typeHandler=com.ruipeng.cloudstorage.config.mybatis.PermissionTypeHandler}, " +
            "#{expiresAt}, #{createdAt}, #{active})")
    int insert(Share share);

    @Select("""
        SELECT * FROM shares
        WHERE id = #{id}
        AND is_active = true
        AND (expires_at IS NULL OR expires_at > NOW())
    """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "fileId", column = "file_id"),
            @Result(property = "createdBy", column = "created_by"),
            @Result(property = "accessType", column = "access_type"),
            @Result(property = "expiresAt", column = "expires_at"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "active", column = "is_active")
    })
    Share findById(UUID id);

    @Select("""
        SELECT * FROM shares
        WHERE file_id = #{fileId}
        AND created_by = #{createdBy}
        AND is_active = true
    """)
    List<Share> findByFileIdAndCreatedBy(
            @Param("fileId") Long fileId,
            @Param("createdBy") Long createdBy
    );

    @Update("""
        UPDATE shares
        SET is_active = false
        WHERE id = #{id,jdbcType=OTHER, typeHandler=com.ruipeng.cloudstorage.config.mybatis.UUIDTypeHandler}
    """)
    int deactivateById(UUID id);

    @Delete("DELETE FROM shares WHERE file_id = #{fileId} AND created_by=#{userId}")
    void deleteByFileIdAndCreatedBy(Long fileId, Long userId);

    @Select("""
        SELECT * FROM shares
        WHERE created_by = #{userId}
    """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "fileId", column = "file_id"),
            @Result(property = "createdBy", column = "created_by"),
            @Result(property = "accessType", column = "access_type"),
            @Result(property = "expiresAt", column = "expires_at"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "active", column = "is_active")
    })
    List<Share> findByUserId(Long userId);
}
