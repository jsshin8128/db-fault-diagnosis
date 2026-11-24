package io.github.jsshin8128.order;

import java.math.BigDecimal;

public record OrderSummary(
		long orderCount,
		BigDecimal totalAmount
) {
}
