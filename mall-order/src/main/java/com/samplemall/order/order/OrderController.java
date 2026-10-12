package com.samplemall.order.order;

import com.samplemall.common.ApiResponse;
import com.samplemall.common.BizException;
import com.samplemall.common.UserHeaders;
import com.samplemall.order.ProductClient.StockLine;
import com.samplemall.order.order.OrderService.Priced;
import com.samplemall.order.order.OrderService.Receiver;
import com.samplemall.order.order.OrderService.Source;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 结算与订单（REQ-040 ~ REQ-044、REQ-050 ~ REQ-052）。 */
@RestController
public class OrderController {

    public record Item(
            @NotBlank String productId,
            @Min(value = 1, message = "数量至少为 1") @Max(value = 99, message = "单件商品最多购买 99 件") int qty) {
    }

    public record PreviewRequest(@NotNull Source source, List<@Valid Item> items) {
    }

    /** 收货信息校验文案与前端行内提示一致（REQ-041）。 */
    public record CreateRequest(
            @NotNull Source source,
            List<@Valid Item> items,
            @NotBlank(message = "请填写收货人") @Size(max = 20, message = "收货人最多 20 个字") String receiverName,
            @NotBlank(message = "请填写手机号") @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确") String receiverPhone,
            @NotBlank(message = "请填写收货地址") @Size(max = 100, message = "收货地址最多 100 个字") String receiverAddress,
            @Size(max = 100, message = "备注最多 100 个字") String remark) {
    }

    public record PreviewView(List<MallOrder.Line> lines, BigDecimal itemsAmount, BigDecimal shippingFee,
                              BigDecimal payAmount) {
        static PreviewView of(Priced p) {
            return new PreviewView(p.lines(), p.itemsAmount(), p.shippingFee(), p.payAmount());
        }
    }

    public record OrderView(
            String orderNo, String status, List<MallOrder.Line> lines, int totalCount, BigDecimal itemsAmount,
            BigDecimal shippingFee, BigDecimal payAmount, String receiverName, String receiverPhone,
            String receiverAddress, String remark, Instant createdAt, Instant paidAt, Instant cancelledAt) {

        static OrderView of(MallOrder o) {
            return new OrderView(o.getOrderNo(), o.getStatus().name(), List.copyOf(o.getLines()),
                    o.getLines().stream().mapToInt(MallOrder.Line::qty).sum(), o.getItemsAmount(),
                    o.getShippingFee(), o.getPayAmount(), o.getReceiverName(), o.getReceiverPhone(),
                    o.getReceiverAddress(), o.getRemark(), o.getCreatedAt(), o.getPaidAt(), o.getCancelledAt());
        }
    }

    private final OrderService service;
    private final OrderRepository orders;

    public OrderController(OrderService service, OrderRepository orders) {
        this.service = service;
        this.orders = orders;
    }

    /** 结算页预览：商品清单、运费与应付金额（不扣库存）。 */
    @PostMapping("/api/orders/preview")
    public ApiResponse<PreviewView> preview(@RequestHeader(UserHeaders.USER_ID) long userId,
                                            @Valid @RequestBody PreviewRequest req) {
        return ApiResponse.ok(PreviewView.of(service.price(userId, req.source(), lines(req.source(), req.items()))));
    }

    @PostMapping("/api/orders")
    public ApiResponse<OrderView> create(@RequestHeader(UserHeaders.USER_ID) long userId,
                                         @Valid @RequestBody CreateRequest req) {
        Receiver receiver = new Receiver(req.receiverName().trim(), req.receiverPhone().trim(),
                req.receiverAddress().trim(), req.remark() == null ? null : req.remark().trim());
        return ApiResponse.ok(OrderView.of(
                service.create(userId, req.source(), lines(req.source(), req.items()), receiver)));
    }

    /** 我的订单（REQ-050）：status 为空返回全部，按下单时间倒序。 */
    @GetMapping("/api/orders")
    public ApiResponse<List<OrderView>> list(@RequestHeader(UserHeaders.USER_ID) long userId,
                                             @RequestParam(required = false) String status) {
        List<MallOrder> rows = (status == null || status.isBlank() || "ALL".equals(status))
                ? orders.findByUserIdOrderByIdDesc(userId)
                : orders.findByUserIdAndStatusOrderByIdDesc(userId, parseStatus(status));
        return ApiResponse.ok(rows.stream().map(OrderView::of).toList());
    }

    @GetMapping("/api/orders/{orderNo}")
    public ApiResponse<OrderView> detail(@RequestHeader(UserHeaders.USER_ID) long userId,
                                         @PathVariable String orderNo) {
        return ApiResponse.ok(OrderView.of(service.get(userId, orderNo)));
    }

    @PostMapping("/api/orders/{orderNo}/pay")
    public ApiResponse<OrderView> pay(@RequestHeader(UserHeaders.USER_ID) long userId,
                                      @PathVariable String orderNo) {
        return ApiResponse.ok(OrderView.of(service.pay(userId, orderNo)));
    }

    @PostMapping("/api/orders/{orderNo}/cancel")
    public ApiResponse<OrderView> cancel(@RequestHeader(UserHeaders.USER_ID) long userId,
                                         @PathVariable String orderNo) {
        return ApiResponse.ok(OrderView.of(service.cancel(userId, orderNo)));
    }

    private static List<StockLine> lines(Source source, List<Item> items) {
        if (source == Source.CART) {
            return List.of();
        }
        if (items == null || items.size() != 1) {
            throw BizException.badRequest("VALIDATION_FAILED", "立即购买仅支持单个商品");
        }
        return items.stream().map(i -> new StockLine(i.productId(), i.qty())).toList();
    }

    private static MallOrder.Status parseStatus(String status) {
        try {
            return MallOrder.Status.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw BizException.badRequest("VALIDATION_FAILED", "未知的订单状态：" + status);
        }
    }
}
