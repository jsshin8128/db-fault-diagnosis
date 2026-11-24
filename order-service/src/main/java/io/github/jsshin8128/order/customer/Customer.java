package io.github.jsshin8128.order.customer;

import java.time.LocalDateTime;

public record Customer(
		long id,
		String name,
		String email,
		String grade,
		LocalDateTime createdAt
) {
}
