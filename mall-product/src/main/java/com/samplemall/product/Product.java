package com.samplemall.product;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OrderColumn;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Product {

    /** 语义化 slug（如 peach-oolong），同时用于路由 /product/:id 与前端 testid。 */
    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(nullable = false, length = 32)
    private String categoryCode;

    /** 纯色块代替商品图：零外链、零加载等待。 */
    @Column(nullable = false, length = 16)
    private String swatch;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /** 划线原价，仅限时特惠商品有值。 */
    @Column(precision = 10, scale = 2)
    private BigDecimal originalPrice;

    @Column(nullable = false)
    private int stock;

    @Column(nullable = false, length = 64)
    private String origin;

    @Column(nullable = false, length = 128)
    private String tagline;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(nullable = false)
    private int sort;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "product_spec")
    @OrderColumn(name = "idx")
    private List<Spec> specs = new ArrayList<>();

    @Embeddable
    public record Spec(@Column(name = "spec_label", length = 32) String label,
                       @Column(name = "spec_value", length = 64) String value) {
    }

    protected Product() {
    }

    public Product(String id, String name, String categoryCode, String swatch, BigDecimal price,
                   BigDecimal originalPrice, int stock, String origin, String tagline, String description,
                   int sort, List<Spec> specs) {
        this.id = id;
        this.name = name;
        this.categoryCode = categoryCode;
        this.swatch = swatch;
        this.price = price;
        this.originalPrice = originalPrice;
        this.stock = stock;
        this.origin = origin;
        this.tagline = tagline;
        this.description = description;
        this.sort = sort;
        this.specs = new ArrayList<>(specs);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public String getSwatch() {
        return swatch;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public int getStock() {
        return stock;
    }

    public String getOrigin() {
        return origin;
    }

    public String getTagline() {
        return tagline;
    }

    public String getDescription() {
        return description;
    }

    public int getSort() {
        return sort;
    }

    public List<Spec> getSpecs() {
        return specs;
    }
}
