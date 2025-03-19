package com.ruipeng.cloudstorage.mappers;


import com.ruipeng.cloudstorage.entity.User;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UserMapper {

    @Select("SELECT * FROM users WHERE email = #{email}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "email", column = "email"),
            @Result(property = "passwordHash", column = "password_hash"),
            @Result(property = "name", column = "name"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    User findByEmail(String email);


    @Insert("INSERT INTO users (email, password_hash, name) " +
            "VALUES (#{email}, #{passwordHash}, #{name})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertUser(User user);

    @Select("SELECT id FROM users WHERE email = #{email}")
    @Results({
            @Result(property = "id", column = "id")
    })
    long getUserId(String email);
}

