package com.ruipeng.cloudstorage.controller;

import com.ruipeng.cloudstorage.entity.User;
import com.ruipeng.cloudstorage.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.HashMap;
import java.util.Map;

@Controller
public class SignupController {
    @Autowired
    private UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<Map<String, String>> signup(@RequestBody User user) {
        System.out.println(user.getName());
        boolean registered = userService.register(user);
        Map<String, String> response = new HashMap<>();
        if (registered) {
            response.put("message", "successfully registered!");
            return  ResponseEntity.ok().body(response);
        }else{
            response.put("message", "failed");
            return  ResponseEntity.badRequest().body(response);
        }

    }
}
