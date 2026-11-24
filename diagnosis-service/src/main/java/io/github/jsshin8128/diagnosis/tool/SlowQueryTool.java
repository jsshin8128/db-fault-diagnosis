package io.github.jsshin8128.diagnosis.tool;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SlowQueryTool implements DiagnosticTool {

	private static final int DEFAULT_LIMIT = 5;
	private static final int MAX_LIMIT = 20;

	private static final String FIND_SLOW_QUERIES = """
			SELECT DIGEST AS digest,
			       DIGEST_TEXT AS query,
			       COUNT_STAR AS calls,
			       ROUND(AVG_TIMER_WAIT / 1000000000, 2) AS avg_ms,
			       ROUND(SUM_ROWS_EXAMINED / COUNT_STAR) AS avg_rows_examined,
			       ROUND(SUM_ROWS_SENT / COUNT_STAR) AS avg_rows_sent
			FROM performance_schema.events_statements_summary_by_digest
			WHERE SCHEMA_NAME IS NOT NULL
			  AND SCHEMA_NAME NOT IN ('mysql', 'sys', 'performance_schema', 'information_schema')
			ORDER BY AVG_TIMER_WAIT DESC
			LIMIT ?""";

	private final JdbcTemplate jdbcTemplate;

	public SlowQueryTool(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public String name() {
		return "get_slow_queries";
	}

	@Override
	public String description() {
		return "Returns the SQL statements executed by the target service, slowest first by average execution time. "
				+ "Each entry has a digest (statement identifier), the normalized statement text, the number of calls, "
				+ "the average execution time in milliseconds, the average number of rows examined, "
				+ "and the average number of rows returned.";
	}

	@Override
	public InputSchema inputSchema() {
		return new InputSchema(
				Map.of("limit", Map.of("type", "integer", "description", "Number of statements to return, 1 to 20. Defaults to 5.")),
				List.of()
		);
	}

	@Override
	public Object execute(Map<String, Object> input) {
		int limit = DEFAULT_LIMIT;
		if (input.get("limit") instanceof Number number) {
			limit = Math.max(1, Math.min(MAX_LIMIT, number.intValue()));
		}
		return jdbcTemplate.queryForList(FIND_SLOW_QUERIES, limit);
	}

}
