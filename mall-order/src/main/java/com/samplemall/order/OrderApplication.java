package com.samplemall.order;

import com.samplemall.common.GlobalExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestClient;

@SpringBootApplication
@Import(GlobalExceptionHandler.class)
public class OrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }

    @Bean
    RestClient productRestClient(@Value("${mall.services.product}") String productUrl) {
        return RestClient.builder().baseUrl(productUrl).build();
    }
}
