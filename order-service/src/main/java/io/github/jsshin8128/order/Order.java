package io.github.jsshin8128.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Order(
		long id,
		long customerId,
		String status,
		BigDecimal totalAmount,
		LocalDateTime orderedAt
) {
}
