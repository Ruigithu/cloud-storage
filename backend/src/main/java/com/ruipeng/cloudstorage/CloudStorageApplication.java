package com.ruipeng.cloudstorage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CloudStorageApplication {

    public static void main(String[] args) {
        System.out.println(org.apache.commons.io.input.BoundedInputStream.class.getProtectionDomain().getCodeSource().getLocation());
        try {
            java.lang.reflect.Method method = org.apache.commons.io.input.BoundedInputStream.class.getMethod("builder");
            System.out.println("方法存在: " + method);
        } catch (NoSuchMethodException e) {
            System.out.println("方法不存在: " + e);
        }
        SpringApplication.run(CloudStorageApplication.class, args);
    }

}
