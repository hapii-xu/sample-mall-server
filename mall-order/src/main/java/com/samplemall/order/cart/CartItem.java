package com.samplemall.order.cart;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "productId"}))
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 64)
    private String productId;

    @Column(nullable = false)
    private int qty;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected CartItem() {
    }

    public CartItem(Long userId, String productId, int qty) {
        this.userId = userId;
        this.productId = productId;
        this.qty = qty;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getProductId() {
        return productId;
    }

    public int getQty() {
        return qty;
    }

    public void setQty(int qty) {
        this.qty = qty;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
