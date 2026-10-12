package com.samplemall.product;

import com.samplemall.common.ApiResponse;
import com.samplemall.common.BizException;
import com.samplemall.product.ProductViews.ProductSummary;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 服务间内部接口（/internal/**，网关不转发）：供交易服务批量取商品快照、扣减与回补库存。
 */
@RestController
public class InternalStockController {

    public record StockLine(@NotBlank String productId, @Min(1) int qty) {
    }

    public record StockRequest(@NotEmpty List<@Valid StockLine> items) {
    }

    private final ProductRepository products;

    public InternalStockController(ProductRepository products) {
        this.products = products;
    }

    @GetMapping("/internal/products")
    public ApiResponse<List<ProductSummary>> batch(@RequestParam List<String> ids) {
        return ApiResponse.ok(products.findByIdIn(ids).stream().map(ProductSummary::of).toList());
    }

    /** 整单扣减：任一商品不足则整体回滚，返回 409 INSUFFICIENT_STOCK（REQ-043）。 */
    @PostMapping("/internal/stock/deduct")
    @Transactional
    public ApiResponse<Void> deduct(@Valid @RequestBody StockRequest req) {
        for (StockLine line : req.items()) {
            if (products.deduct(line.productId(), line.qty()) == 0) {
                String name = products.findById(line.productId()).map(Product::getName).orElse(line.productId());
                throw BizException.conflict("INSUFFICIENT_STOCK", "「" + name + "」库存不足");
            }
        }
        return ApiResponse.ok();
    }

    /** 取消订单时回补库存（REQ-052）。 */
    @PostMapping("/internal/stock/restore")
    @Transactional
    public ApiResponse<Void> restore(@Valid @RequestBody StockRequest req) {
        req.items().forEach(line -> products.restore(line.productId(), line.qty()));
        return ApiResponse.ok();
    }
}
