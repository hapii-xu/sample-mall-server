package com.samplemall.order.order;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "mall_order")
public class MallOrder {

    /** 订单状态（v0.1）：待支付 → 已支付；待支付 → 已取消。 */
    public enum Status {
        PENDING_PAYMENT, PAID, CANCELLED
    }

    /** 下单时的商品快照，后续商品改价不影响已下订单。 */
    @Embeddable
    public record Line(
            @Column(length = 64) String productId,
            @Column(length = 64) String productName,
            @Column(length = 16) String swatch,
            @Column(precision = 10, scale = 2) BigDecimal price,
            int qty) {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 32)
    private String orderNo;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING_PAYMENT;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "mall_order_line")
    @OrderColumn(name = "idx")
    private List<Line> lines = new ArrayList<>();

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal itemsAmount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal shippingFee;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal payAmount;

    @Column(nullable = false, length = 32)
    private String receiverName;

    @Column(nullable = false, length = 16)
    private String receiverPhone;

    @Column(nullable = false, length = 200)
    private String receiverAddress;

    @Column(length = 200)
    private String remark;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private Instant paidAt;

    private Instant cancelledAt;

    protected MallOrder() {
    }

    public MallOrder(Long userId, List<Line> lines, BigDecimal itemsAmount, BigDecimal shippingFee,
                     String receiverName, String receiverPhone, String receiverAddress, String remark) {
        this.userId = userId;
        this.lines = new ArrayList<>(lines);
        this.itemsAmount = itemsAmount;
        this.shippingFee = shippingFee;
        this.payAmount = itemsAmount.add(shippingFee);
        this.receiverName = receiverName;
        this.receiverPhone = receiverPhone;
        this.receiverAddress = receiverAddress;
        this.remark = remark;
    }

    public void assignOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public void markPaid() {
        this.status = Status.PAID;
        this.paidAt = Instant.now();
    }

    public void markCancelled() {
        this.status = Status.CANCELLED;
        this.cancelledAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public Long getUserId() {
        return userId;
    }

    public Status getStatus() {
        return status;
    }

    public List<Line> getLines() {
        return lines;
    }

    public BigDecimal getItemsAmount() {
        return itemsAmount;
    }

    public BigDecimal getShippingFee() {
        return shippingFee;
    }

    public BigDecimal getPayAmount() {
        return payAmount;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public String getReceiverPhone() {
        return receiverPhone;
    }

    public String getReceiverAddress() {
        return receiverAddress;
    }

    public String getRemark() {
        return remark;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }
}
