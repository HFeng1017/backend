package com.resume.platform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.resume.platform.mapper")
public class ResumePlatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(ResumePlatformApplication.class, args);
    }
}
