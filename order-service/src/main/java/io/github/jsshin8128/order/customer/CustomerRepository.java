package io.github.jsshin8128.order.customer;

import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CustomerRepository {

	private static final String FIND_BY_ID = """
			SELECT id, name, email, grade, created_at
			FROM customers
			WHERE id = ?""";

	private final JdbcTemplate jdbcTemplate;

	public CustomerRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public Optional<Customer> findById(long id) {
		return jdbcTemplate.query(FIND_BY_ID, (rs, rowNum) -> new Customer(
				rs.getLong("id"),
				rs.getString("name"),
				rs.getString("email"),
				rs.getString("grade"),
				rs.getTimestamp("created_at").toLocalDateTime()
		), id).stream().findFirst();
	}

}
