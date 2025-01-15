package com.ruipeng.cloudstorage.service;

import com.ruipeng.cloudstorage.entity.User;
import com.ruipeng.cloudstorage.mappers.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder encoder;


    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public boolean register(User user){
        User findUser = userMapper.findByUsername(user.getUsername());
        if(findUser != null){
            System.out.println("User already exists");
        }else{
            System.out.println("username:"+user.getUsername());
            if (user.getPassword() == null || user.getPassword().isEmpty()) {
                throw new IllegalArgumentException("the password can not be empty");
            }
            user.setPassword(encoder.encode(user.getPassword()));
            int i = userMapper.insertUser(user);
            System.out.println(i);
            if(i == 1){
                return true;
            }
        }
        return false;
    }

}
