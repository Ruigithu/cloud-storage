package com.ruipeng.cloudstorage.mappers;

import com.ruipeng.cloudstorage.entity.File;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface FileMapper {
    @Select("SELECT * FROM FILES WHERE userid = #{userid}")
    @Results({
            @Result(property = "fileid", column = "fileid"),
            @Result(property = "filename", column = "filename"),
            @Result(property = "contenttype", column = "contenttype"),
            @Result(property = "filesize", column = "filesize"),
            @Result(property = "userid", column = "userid"),
            @Result(property = "filedata", column = "filedata"),
    })
    List<File> getFilesByUserId(int userid);

    @Insert("INSERT INTO files (filename,contenttype,filesize,userid,filedata)"+
            "VALUES (#{filename},#{contenttype},#{filesize},#{userid},#{filedata})")
    @Options(useGeneratedKeys = true, keyProperty = "fileid")
    int insert(File file);

    @Delete("DELETE FROM files WHERE fileid=#{fileid}")
    int deleteByFileid(int fileid);
}
