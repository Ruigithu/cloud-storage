package com.ruipeng.cloudstorage.config.security;

import com.ruipeng.cloudstorage.entity.User;
import com.ruipeng.cloudstorage.mappers.UserMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AppUserDetailsService implements UserDetailsService {


    private final UserMapper userMapper;

    public AppUserDetailsService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("loadByUsername: "+username);
        User user = userMapper.findByEmail(username);
        if (user == null) {
            System.out.println("user not found");
            throw new UsernameNotFoundException(username);
        }else {
            return new AppUserDetails(user);
        }
    }
}
