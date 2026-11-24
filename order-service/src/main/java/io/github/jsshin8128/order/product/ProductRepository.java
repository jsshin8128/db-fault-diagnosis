package io.github.jsshin8128.order.product;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class ProductRepository {

	private static final String FIND_BY_ID = """
			SELECT id, name, category, price
			FROM products
			WHERE id = ?""";

	private static final String FIND_BY_CATEGORY = """
			SELECT id, name, category, price
			FROM products
			WHERE category = ?
			ORDER BY id
			LIMIT 20""";

	private static final String COUNT_BY_CATEGORY = """
			SELECT category, COUNT(*) AS product_count
			FROM products
			GROUP BY category
			ORDER BY category""";

	private static final RowMapper<Product> PRODUCT_MAPPER = (rs, rowNum) -> new Product(
			rs.getLong("id"),
			rs.getString("name"),
			rs.getString("category"),
			rs.getBigDecimal("price")
	);

	private final JdbcTemplate jdbcTemplate;

	public ProductRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public Optional<Product> findById(long id) {
		return jdbcTemplate.query(FIND_BY_ID, PRODUCT_MAPPER, id).stream().findFirst();
	}

	public List<Product> findByCategory(String category) {
		return jdbcTemplate.query(FIND_BY_CATEGORY, PRODUCT_MAPPER, category);
	}

	public Map<String, Long> countByCategory() {
		Map<String, Long> counts = new LinkedHashMap<>();
		jdbcTemplate.query(COUNT_BY_CATEGORY, rs -> {
			counts.put(rs.getString("category"), rs.getLong("product_count"));
		});
		return counts;
	}

}
