package com.samplemall.order.cart;

import com.samplemall.common.ApiResponse;
import com.samplemall.common.BizException;
import com.samplemall.common.UserHeaders;
import com.samplemall.order.Pricing;
import com.samplemall.order.ProductClient;
import com.samplemall.order.ProductClient.ProductSnapshot;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** 购物车（REQ-020 / REQ-021 / REQ-022），按登录用户隔离，服务端持久化。 */
@RestController
public class CartController {

    public record AddRequest(
            @NotBlank(message = "请选择商品") String productId,
            @Min(value = 1, message = "数量至少为 1") @Max(value = 99, message = "单件商品最多购买 99 件") int qty) {
    }

    public record UpdateRequest(
            @Min(value = 1, message = "数量至少为 1") @Max(value = 99, message = "单件商品最多购买 99 件") int qty) {
    }

    /** available=false 时 unavailableReason 给出原因（已售罄 / 库存不足 / 已下架），该行不参与结算。 */
    public record CartLine(
            String productId, String name, String swatch, BigDecimal price, int qty, int stock,
            String stockStatus, BigDecimal subtotal, boolean available, String unavailableReason) {
    }

    public record CartView(
            List<CartLine> items, int totalCount, BigDecimal itemsAmount, BigDecimal shippingFee,
            BigDecimal payAmount) {
    }

    private final CartItemRepository cart;
    private final ProductClient productClient;

    public CartController(CartItemRepository cart, ProductClient productClient) {
        this.cart = cart;
        this.productClient = productClient;
    }

    @GetMapping("/api/cart")
    public ApiResponse<CartView> get(@RequestHeader(UserHeaders.USER_ID) long userId) {
        return ApiResponse.ok(view(userId));
    }

    /** 加入购物车：同一商品合并数量，上限 99；售罄商品不可加入。 */
    @PostMapping("/api/cart/items")
    public ApiResponse<CartView> add(@RequestHeader(UserHeaders.USER_ID) long userId,
                                     @Valid @RequestBody AddRequest req) {
        ProductSnapshot p = productClient.snapshots(List.of(req.productId())).get(req.productId());
        if (p == null) {
            throw BizException.notFound("PRODUCT_NOT_FOUND", "商品不存在或已下架");
        }
        if (p.stock() <= 0) {
            throw BizException.conflict("SOLD_OUT", "商品已售罄");
        }
        CartItem item = cart.findByUserIdAndProductId(userId, req.productId())
                .orElseGet(() -> new CartItem(userId, req.productId(), 0));
        item.setQty(Math.min(Pricing.MAX_QTY, item.getQty() + req.qty()));
        cart.save(item);
        return ApiResponse.ok(view(userId));
    }

    @PutMapping("/api/cart/items/{productId}")
    public ApiResponse<CartView> update(@RequestHeader(UserHeaders.USER_ID) long userId,
                                        @PathVariable String productId,
                                        @Valid @RequestBody UpdateRequest req) {
        CartItem item = cart.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> BizException.notFound("CART_ITEM_NOT_FOUND", "购物车中没有该商品"));
        item.setQty(req.qty());
        cart.save(item);
        return ApiResponse.ok(view(userId));
    }

    @DeleteMapping("/api/cart/items/{productId}")
    public ApiResponse<CartView> remove(@RequestHeader(UserHeaders.USER_ID) long userId,
                                        @PathVariable String productId) {
        cart.deleteByUserIdAndProductId(userId, productId);
        return ApiResponse.ok(view(userId));
    }

    @DeleteMapping("/api/cart")
    public ApiResponse<CartView> clear(@RequestHeader(UserHeaders.USER_ID) long userId) {
        cart.deleteByUserId(userId);
        return ApiResponse.ok(view(userId));
    }

    CartView view(long userId) {
        List<CartItem> items = cart.findByUserIdOrderByCreatedAtAsc(userId);
        Map<String, ProductSnapshot> products =
                productClient.snapshots(items.stream().map(CartItem::getProductId).toList());
        List<CartLine> lines = items.stream().map(i -> line(i, products.get(i.getProductId()))).toList();
        BigDecimal itemsAmount = lines.stream()
                .filter(CartLine::available)
                .map(CartLine::subtotal)
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
        BigDecimal shipping = Pricing.shippingFee(itemsAmount);
        int totalCount = items.stream().mapToInt(CartItem::getQty).sum();
        return new CartView(lines, totalCount, itemsAmount, shipping, itemsAmount.add(shipping));
    }

    private static CartLine line(CartItem item, ProductSnapshot p) {
        if (p == null) {
            return new CartLine(item.getProductId(), "已下架商品", "#cccccc", BigDecimal.ZERO.setScale(2),
                    item.getQty(), 0, "SOLD_OUT", BigDecimal.ZERO.setScale(2), false, "商品已下架");
        }
        String reason = null;
        if (p.stock() <= 0) {
            reason = "已售罄";
        } else if (item.getQty() > p.stock()) {
            reason = "库存不足，仅剩 " + p.stock() + " 件";
        }
        BigDecimal subtotal = p.price().multiply(BigDecimal.valueOf(item.getQty()));
        return new CartLine(p.id(), p.name(), p.swatch(), p.price(), item.getQty(), p.stock(), p.stockStatus(),
                subtotal, reason == null, reason);
    }
}
