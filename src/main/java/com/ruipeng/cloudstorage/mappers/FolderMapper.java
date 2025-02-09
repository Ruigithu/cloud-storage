package com.ruipeng.cloudstorage.mappers;

import com.ruipeng.cloudstorage.entity.File;
import com.ruipeng.cloudstorage.entity.Folder;
import org.apache.ibatis.annotations.*;
import org.postgresql.util.PGobject;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

@Mapper
public interface FolderMapper {
    @Insert("INSERT INTO folders ( name, parent_id, owner_id, path, created_at, updated_at, is_deleted) " +
            "VALUES ( #{name}, #{parentId}, #{ownerId}, " +
            "#{path,typeHandler=com.ruipeng.cloudstorage.config.mybatis.LtreeTypeHandler}, " +
            "#{createdAt,jdbcType=TIMESTAMP}, #{updatedAt,jdbcType=TIMESTAMP}, #{isDeleted})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "parentId", column = "parent_id"),
            @Result(property = "ownerId", column = "owner_id"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "isDeleted", column = "is_deleted")
    })
    int insert(Folder folder);

    @Select("SELECT * FROM folders WHERE id = #{id} AND is_deleted = false")
    Folder findById(@Param("id") Long id);

    @Select("SELECT * FROM folders WHERE id = #{id} AND is_deleted = true")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "parentId", column = "parent_id"),
            @Result(property = "ownerId", column = "owner_id"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "isDeleted", column = "is_deleted")
    })
    Folder findDeletedById(@Param("id") Long id);


    @Update("UPDATE folders SET path = #{path,typeHandler=com.ruipeng.cloudstorage.config.mybatis.LtreeTypeHandler}, " +
            "updated_at = #{updatedAt} WHERE id = #{id}")
    void updatePath(@Param("id") Long id, @Param("path") PGobject path, @Param("updatedAt") Instant updatedAt);


    @Select("SELECT EXISTS(SELECT 1 FROM folders WHERE id = #{id} AND owner_id=#{userId})")
    boolean existsById(@Param("id") Long id, Long userId);

    @Select("SELECT * FROM folders WHERE owner_id = #{ownerId} AND parent_id=#{parentId} AND is_deleted = FALSE")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "name", column = "name"),
            @Result(property = "ownerId", column = "owner_id"),
            @Result(property = "parentId", column = "parent_id"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "path", column = "path"),
            @Result(property = "isDeleted", column = "is_deleted")
    })
    List<Folder> getFoldersByUserIdAndFolderId(long ownerId, long parentId);

    @Select("SELECT * FROM folders " +
            "WHERE path <@ #{path} ::ltree " +
            "AND id != #{folderId} " +
            "ORDER BY path DESC")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "name", column = "name"),
            @Result(property = "ownerId", column = "owner_id"),
            @Result(property = "parentId", column = "parent_id"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "path", column = "path"),
            @Result(property = "isDeleted", column = "is_deleted")
    })
    List<Folder> findSubFolders(@Param("path") PGobject path, @Param("folderId") long folderId);


    @Delete("DELETE FROM folders WHERE id=#{id}")
    int deleteFolder(Long id);

    @Select("SELECT * FROM folders WHERE owner_id = #{ownerId} AND parent_id=#{parentId} AND is_deleted = TRUE")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "name", column = "name"),
            @Result(property = "ownerId", column = "owner_id"),
            @Result(property = "parentId", column = "parent_id"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "path", column = "path"),
            @Result(property = "isDeleted", column = "is_deleted")
    })
    List<Folder> getDeletedFoldersByUserIdAndFolderId(long ownerId, long parentId);

    @Update("UPDATE folders SET " +
            "name=#{name}," +
            "parent_id=#{parentId}, " +
            "owner_id=#{ownerId}, " +
            "path=#{path,typeHandler=com.ruipeng.cloudstorage.config.mybatis.LtreeTypeHandler}, " +
            "updated_at=#{updatedAt,jdbcType=TIMESTAMP}, " +
            "is_deleted=#{isDeleted} " )
    int updateFolder(Folder folder);

    @Update("UPDATE folders SET is_deleted = #{isDeleted}, updated_at = #{updatedAt} WHERE id = #{id}")
    int updateFolderDeleteStatus(Folder folder);

    @Select("select * from folders where id=#{folderId}")
    Folder getFolderByFolderId(Long folderId);

    @Select("SELECT id FROM folders WHERE owner_id = #{userId} AND parent_id IS NULL LIMIT 1")
    Long getUserRootFolderId(Long userId);
}
