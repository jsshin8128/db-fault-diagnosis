package io.github.jsshin8128.order.product;

import java.math.BigDecimal;

public record Product(
		long id,
		String name,
		String category,
		BigDecimal price
) {
}
