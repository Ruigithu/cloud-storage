package com.ruipeng.cloudstorage.util;

import com.ruipeng.cloudstorage.entity.User;
import com.ruipeng.cloudstorage.mappers.UserMapper;
import com.ruipeng.cloudstorage.config.security.AppUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtil {


    private static ApplicationContext context;

    @Autowired
    public void setApplicationContext(ApplicationContext applicationContext) {
        context = applicationContext;
    }


    @Bean
    public static int getCurrentUserId(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication!=null && authentication.getPrincipal() instanceof AppUserDetails){
            UserDetails userDetails = (AppUserDetails)authentication.getPrincipal();
            System.out.println(userDetails.getUsername());
            UserMapper userMapper = context.getBean(UserMapper.class);
            User u = userMapper.findByUsername(userDetails.getUsername());
            return u.getUserid();
        }
        return -1;
    }
}
