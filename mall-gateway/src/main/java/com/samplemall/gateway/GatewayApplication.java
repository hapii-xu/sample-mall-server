package com.samplemall.gateway;

import com.samplemall.common.JwtSupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

    @Bean
    JwtSupport jwtSupport(@Value("${mall.jwt.secret}") String secret) {
        return new JwtSupport(secret);
    }

    /** 路由表：对外统一 /api 前缀，按业务域转发到各微服务。 */
    @Bean
    RouteLocator routes(
            RouteLocatorBuilder builder,
            @Value("${mall.services.user}") String userUri,
            @Value("${mall.services.product}") String productUri,
            @Value("${mall.services.order}") String orderUri) {
        return builder.routes()
                .route("user", r -> r.path("/api/auth/**", "/api/users/**").uri(userUri))
                .route("product", r -> r.path("/api/categories/**", "/api/products/**").uri(productUri))
                .route("order", r -> r.path("/api/cart/**", "/api/orders/**").uri(orderUri))
                .build();
    }
}
