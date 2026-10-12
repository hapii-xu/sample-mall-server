package com.samplemall.order.order;

import com.samplemall.common.BizException;
import com.samplemall.order.Pricing;
import com.samplemall.order.ProductClient;
import com.samplemall.order.ProductClient.ProductSnapshot;
import com.samplemall.order.ProductClient.StockLine;
import com.samplemall.order.cart.CartItemRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    /** 结算来源：购物车整单 / 详情页「立即购买」。 */
    public enum Source {
        CART, DIRECT
    }

    public record Priced(List<MallOrder.Line> lines, BigDecimal itemsAmount, BigDecimal shippingFee) {
        public BigDecimal payAmount() {
            return itemsAmount.add(shippingFee);
        }
    }

    public record Receiver(String name, String phone, String address, String remark) {
    }

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final OrderRepository orders;
    private final CartItemRepository cart;
    private final ProductClient productClient;

    public OrderService(OrderRepository orders, CartItemRepository cart, ProductClient productClient) {
        this.orders = orders;
        this.cart = cart;
        this.productClient = productClient;
    }

    /**
     * 计算结算清单：CART 取购物车中可结算的行，DIRECT 取请求中的单个商品。
     * 售罄 / 库存不足的商品在此直接拒绝（REQ-043）。
     */
    public Priced price(long userId, Source source, List<StockLine> directItems) {
        List<StockLine> wanted = source == Source.CART
                ? cart.findByUserIdOrderByCreatedAtAsc(userId).stream()
                        .map(i -> new StockLine(i.getProductId(), i.getQty())).toList()
                : directItems;
        if (wanted == null || wanted.isEmpty()) {
            throw BizException.badRequest("NOTHING_TO_SETTLE", "购物车中没有可结算的商品");
        }
        Map<String, ProductSnapshot> products =
                productClient.snapshots(wanted.stream().map(StockLine::productId).toList());
        List<StockLine> settle = source == Source.CART
                ? wanted.stream().filter(w -> available(products.get(w.productId()), w.qty())).toList()
                : wanted;
        if (settle.isEmpty()) {
            throw BizException.badRequest("NOTHING_TO_SETTLE", "购物车中没有可结算的商品");
        }
        List<MallOrder.Line> lines = settle.stream().map(w -> {
            ProductSnapshot p = products.get(w.productId());
            if (p == null) {
                throw BizException.notFound("PRODUCT_NOT_FOUND", "商品不存在或已下架");
            }
            if (!available(p, w.qty())) {
                throw BizException.conflict("INSUFFICIENT_STOCK", "「" + p.name() + "」库存不足");
            }
            return new MallOrder.Line(p.id(), p.name(), p.swatch(), p.price(), w.qty());
        }).toList();
        BigDecimal itemsAmount = lines.stream()
                .map(l -> l.price().multiply(BigDecimal.valueOf(l.qty())))
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
        return new Priced(lines, itemsAmount, Pricing.shippingFee(itemsAmount));
    }

    /** 提交订单（REQ-042）：扣库存 → 落单 → 移除购物车中已结算的商品。 */
    @Transactional
    public MallOrder create(long userId, Source source, List<StockLine> directItems, Receiver receiver) {
        Priced priced = price(userId, source, directItems);
        List<StockLine> stock = priced.lines().stream().map(l -> new StockLine(l.productId(), l.qty())).toList();
        productClient.deduct(stock);
        try {
            MallOrder order = orders.save(new MallOrder(userId, priced.lines(), priced.itemsAmount(),
                    priced.shippingFee(), receiver.name(), receiver.phone(), receiver.address(), receiver.remark()));
            order.assignOrderNo("SM" + LocalDate.now(ZoneId.of("Asia/Shanghai")).format(DAY)
                    + String.format("%06d", order.getId()));
            if (source == Source.CART) {
                cart.deleteByUserIdAndProductIdIn(userId, stock.stream().map(StockLine::productId).toList());
            }
            return order;
        } catch (RuntimeException e) {
            productClient.restore(stock); // 补偿：本地落单失败时归还已扣库存
            throw e;
        }
    }

    public MallOrder get(long userId, String orderNo) {
        return orders.findByOrderNoAndUserId(orderNo, userId)
                .orElseThrow(() -> BizException.notFound("ORDER_NOT_FOUND", "订单不存在"));
    }

    /** 模拟支付（REQ-044）：仅待支付订单可支付。 */
    @Transactional
    public MallOrder pay(long userId, String orderNo) {
        MallOrder order = get(userId, orderNo);
        requirePending(order);
        order.markPaid();
        return orders.save(order);
    }

    /** 取消订单（REQ-052）：仅待支付订单可取消，并回补库存。 */
    @Transactional
    public MallOrder cancel(long userId, String orderNo) {
        MallOrder order = get(userId, orderNo);
        requirePending(order);
        order.markCancelled();
        productClient.restore(order.getLines().stream().map(l -> new StockLine(l.productId(), l.qty())).toList());
        return orders.save(order);
    }

    private static void requirePending(MallOrder order) {
        if (order.getStatus() != MallOrder.Status.PENDING_PAYMENT) {
            throw BizException.conflict("ORDER_STATUS_INVALID", "订单状态已变更，请刷新后重试");
        }
    }

    private static boolean available(ProductSnapshot p, int qty) {
        return p != null && p.stock() > 0 && qty <= p.stock();
    }
}
