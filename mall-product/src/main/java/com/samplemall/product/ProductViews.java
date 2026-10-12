package com.samplemall.product;

import java.math.BigDecimal;
import java.util.List;

/** 对外视图（与前端类型 src/api/types.ts 对应）。 */
public final class ProductViews {

    /** 低库存阈值：库存 ≤ 10 显示「仅剩 N 件」（REQ-012）。 */
    public static final int LOW_STOCK_THRESHOLD = 10;

    private ProductViews() {
    }

    public record CategoryView(String code, String name) {
        static CategoryView of(Category c) {
            return new CategoryView(c.getCode(), c.getName());
        }
    }

    public record ProductSummary(
            String id,
            String name,
            String categoryCode,
            String swatch,
            BigDecimal price,
            BigDecimal originalPrice,
            int stock,
            String stockStatus,
            String tagline) {

        static ProductSummary of(Product p) {
            return new ProductSummary(p.getId(), p.getName(), p.getCategoryCode(), p.getSwatch(), p.getPrice(),
                    p.getOriginalPrice(), p.getStock(), statusOf(p.getStock()), p.getTagline());
        }
    }

    public record ProductDetail(
            String id,
            String name,
            String categoryCode,
            String categoryName,
            String swatch,
            BigDecimal price,
            BigDecimal originalPrice,
            int stock,
            String stockStatus,
            String origin,
            String tagline,
            String description,
            List<Product.Spec> specs) {

        static ProductDetail of(Product p, String categoryName) {
            return new ProductDetail(p.getId(), p.getName(), p.getCategoryCode(), categoryName, p.getSwatch(),
                    p.getPrice(), p.getOriginalPrice(), p.getStock(), statusOf(p.getStock()), p.getOrigin(),
                    p.getTagline(), p.getDescription(), List.copyOf(p.getSpecs()));
        }
    }

    /** IN_STOCK / LOW_STOCK / SOLD_OUT */
    static String statusOf(int stock) {
        if (stock <= 0) {
            return "SOLD_OUT";
        }
        return stock <= LOW_STOCK_THRESHOLD ? "LOW_STOCK" : "IN_STOCK";
    }
}
