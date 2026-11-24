package io.github.jsshin8128.order;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class OrderRepository {

	private static final String FIND_BY_CUSTOMER_ID = """
			SELECT id, customer_id, status, total_amount, ordered_at
			FROM orders
			WHERE customer_id = ?""";

	private static final String FIND_BY_ID = """
			SELECT id, customer_id, status, total_amount, ordered_at
			FROM orders
			WHERE id = ?""";

	private static final String FIND_RECENT = """
			SELECT id, customer_id, status, total_amount, ordered_at
			FROM orders
			ORDER BY id DESC
			LIMIT 20""";

	private static final String FIND_LATEST_BY_CUSTOMER_ID = """
			SELECT id, customer_id, status, total_amount, ordered_at
			FROM orders
			WHERE customer_id = ?
			ORDER BY id DESC
			LIMIT 1""";

	private static final String SUMMARIZE_BY_CUSTOMER_ID = """
			SELECT COUNT(*) AS order_count, COALESCE(SUM(total_amount), 0) AS total_amount
			FROM orders
			WHERE customer_id = ?""";

	private static final String COUNT_BY_ORDERED_ON = """
			SELECT COUNT(*)
			FROM orders
			WHERE DATE(ordered_at) = ?""";

	private static final String UPDATE_STATUS = """
			UPDATE orders
			SET status = ?
			WHERE id = ?""";

	private static final RowMapper<Order> ORDER_MAPPER = (rs, rowNum) -> new Order(
			rs.getLong("id"),
			rs.getLong("customer_id"),
			rs.getString("status"),
			rs.getBigDecimal("total_amount"),
			rs.getTimestamp("ordered_at").toLocalDateTime()
	);

	private final JdbcTemplate jdbcTemplate;

	public OrderRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public List<Order> findByCustomerId(long customerId) {
		return jdbcTemplate.query(FIND_BY_CUSTOMER_ID, ORDER_MAPPER, customerId);
	}

	public Optional<Order> findById(long id) {
		return jdbcTemplate.query(FIND_BY_ID, ORDER_MAPPER, id).stream().findFirst();
	}

	public List<Order> findRecent() {
		return jdbcTemplate.query(FIND_RECENT, ORDER_MAPPER);
	}

	public Optional<Order> findLatestByCustomerId(long customerId) {
		return jdbcTemplate.query(FIND_LATEST_BY_CUSTOMER_ID, ORDER_MAPPER, customerId).stream().findFirst();
	}

	public OrderSummary summarizeByCustomerId(long customerId) {
		return jdbcTemplate.queryForObject(SUMMARIZE_BY_CUSTOMER_ID,
				(rs, rowNum) -> new OrderSummary(rs.getLong("order_count"), rs.getBigDecimal("total_amount")),
				customerId);
	}

	public long countByOrderedOn(LocalDate orderedOn) {
		return jdbcTemplate.queryForObject(COUNT_BY_ORDERED_ON, Long.class, orderedOn);
	}

	public boolean updateStatus(long id, String status) {
		return jdbcTemplate.update(UPDATE_STATUS, status, id) > 0;
	}

}
