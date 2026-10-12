package com.samplemall.order;

import java.math.BigDecimal;

/** 运费规则（REQ-041）：商品金额满 ¥99 包邮，否则运费 ¥10。 */
public final class Pricing {

    public static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("99.00");
    public static final BigDecimal SHIPPING_FEE = new BigDecimal("10.00");
    public static final int MAX_QTY = 99;

    private Pricing() {
    }

    public static BigDecimal shippingFee(BigDecimal itemsAmount) {
        if (itemsAmount.signum() == 0 || itemsAmount.compareTo(FREE_SHIPPING_THRESHOLD) >= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return SHIPPING_FEE;
    }
}
