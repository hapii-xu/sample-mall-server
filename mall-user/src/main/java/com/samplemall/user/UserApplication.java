package com.samplemall.user;

import com.samplemall.common.GlobalExceptionHandler;
import com.samplemall.common.JwtSupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(GlobalExceptionHandler.class)
public class UserApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserApplication.class, args);
    }

    @Bean
    JwtSupport jwtSupport(@Value("${mall.jwt.secret}") String secret) {
        return new JwtSupport(secret);
    }
}
