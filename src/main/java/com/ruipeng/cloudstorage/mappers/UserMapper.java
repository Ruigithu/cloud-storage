package com.ruipeng.cloudstorage.mappers;

import com.ruipeng.cloudstorage.entity.User;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UserMapper {
    @Select("SELECT * FROM USERS WHERE username = #{username}")
    @Results({
            @Result(property = "userid", column = "userid"),
            @Result(property = "username", column = "username"),
            @Result(property = "salt", column = "salt"),
            @Result(property = "password", column = "password"),
            @Result(property = "firstname", column = "firstname"),
            @Result(property = "lastname", column = "lastname")
    })
    User findByUsername(String username);

    @Insert("INSERT INTO USERS (username, salt, password, firstname, lastname ) " +
            "VALUES (#{username}, #{salt}, #{password}, #{firstname}, #{lastname})")
    @Options(useGeneratedKeys = true, keyProperty = "userid")
    int insertUser(User user);
}
