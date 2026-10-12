package com.samplemall.gateway;

import com.samplemall.common.JwtSupport;
import com.samplemall.common.UserHeaders;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 全局鉴权：
 * <ul>
 *   <li>先剥离外部伪造的身份头（下游只信任网关注入的值）；</li>
 *   <li>公开接口（登录、商品与分类查询）放行，带了有效令牌也会注入身份；</li>
 *   <li>其余接口必须携带有效 {@code Authorization: Bearer <token>}，否则 401。</li>
 * </ul>
 */
@Component
public class AuthFilter implements GlobalFilter, Ordered {

    private static final AntPathMatcher MATCHER = new AntPathMatcher();
    private static final List<String> PUBLIC_GET = List.of("/api/products/**", "/api/categories/**");
    private static final List<String> PUBLIC_ANY = List.of("/api/auth/login");

    private final JwtSupport jwt;

    public AuthFilter(JwtSupport jwt) {
        this.jwt = jwt;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        JwtSupport.Claims claims = jwt.verify(bearer(request.getHeaders()));

        ServerHttpRequest.Builder mutated = request.mutate().headers(h -> {
            h.remove(UserHeaders.USER_ID);
            h.remove(UserHeaders.USERNAME);
            if (claims != null) {
                h.set(UserHeaders.USER_ID, String.valueOf(claims.userId()));
                h.set(UserHeaders.USERNAME, claims.username());
            }
        });
        if (claims == null && !isPublic(request.getMethod(), path)) {
            return unauthorized(exchange.getResponse());
        }
        return chain.filter(exchange.mutate().request(mutated.build()).build());
    }

    private static boolean isPublic(HttpMethod method, String path) {
        if (PUBLIC_ANY.stream().anyMatch(p -> MATCHER.match(p, path))) {
            return true;
        }
        return HttpMethod.GET.equals(method) && PUBLIC_GET.stream().anyMatch(p -> MATCHER.match(p, path));
    }

    private static String bearer(HttpHeaders headers) {
        String value = headers.getFirst(HttpHeaders.AUTHORIZATION);
        return value != null && value.startsWith("Bearer ") ? value.substring(7) : null;
    }

    private static Mono<Void> unauthorized(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = "{\"code\":\"UNAUTHORIZED\",\"message\":\"请先登录\",\"data\":null}"
                .getBytes(StandardCharsets.UTF_8);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
