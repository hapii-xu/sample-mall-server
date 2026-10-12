package com.samplemall.order;

import com.samplemall.common.ApiResponse;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** 调用商品服务内部接口（/internal/**）。下游 4xx 由全局异常处理原样透传给前端。 */
@Component
public class ProductClient {

    /** 商品快照（字段与商品服务 ProductSummary 对齐）。 */
    public record ProductSnapshot(
            String id, String name, String swatch, BigDecimal price, int stock, String stockStatus) {
    }

    public record StockLine(String productId, int qty) {
    }

    private record StockRequest(List<StockLine> items) {
    }

    private final RestClient client;

    public ProductClient(RestClient productRestClient) {
        this.client = productRestClient;
    }

    public Map<String, ProductSnapshot> snapshots(Collection<String> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        ApiResponse<List<ProductSnapshot>> body = client.get()
                .uri(b -> b.path("/internal/products").queryParam("ids", String.join(",", ids)).build())
                .retrieve()
                .body(new ParameterizedTypeReference<ApiResponse<List<ProductSnapshot>>>() { });
        return body.data().stream().collect(Collectors.toMap(ProductSnapshot::id, Function.identity()));
    }

    public void deduct(List<StockLine> lines) {
        client.post().uri("/internal/stock/deduct").body(new StockRequest(lines)).retrieve().toBodilessEntity();
    }

    public void restore(List<StockLine> lines) {
        client.post().uri("/internal/stock/restore").body(new StockRequest(lines)).retrieve().toBodilessEntity();
    }
}
