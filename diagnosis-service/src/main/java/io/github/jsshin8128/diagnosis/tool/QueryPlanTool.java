package io.github.jsshin8128.diagnosis.tool;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class QueryPlanTool implements DiagnosticTool {

	private static final String FIND_SAMPLE_BY_DIGEST = """
			SELECT SCHEMA_NAME AS schema_name,
			       QUERY_SAMPLE_TEXT AS sample
			FROM performance_schema.events_statements_summary_by_digest
			WHERE DIGEST = ?
			  AND SCHEMA_NAME IS NOT NULL
			  AND SCHEMA_NAME NOT IN ('mysql', 'sys', 'performance_schema', 'information_schema')
			LIMIT 1""";

	private final JdbcTemplate jdbcTemplate;

	public QueryPlanTool(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public String name() {
		return "get_query_plan";
	}

	@Override
	public String description() {
		return "Returns the execution plan of one SQL statement, identified by the digest returned from get_slow_queries. "
				+ "The plan is produced with EXPLAIN FORMAT=TREE for a sample of the statement; the statement itself is not executed.";
	}

	@Override
	public InputSchema inputSchema() {
		return new InputSchema(
				Map.of("digest", Map.of("type", "string", "description", "Statement digest from get_slow_queries")),
				List.of("digest")
		);
	}

	@Override
	public Object execute(Map<String, Object> input) {
		String digest = String.valueOf(input.get("digest"));
		List<Map<String, Object>> rows = jdbcTemplate.queryForList(FIND_SAMPLE_BY_DIGEST, digest);
		if (rows.isEmpty()) {
			throw new IllegalArgumentException("No statement found for digest " + digest);
		}
		String schema = String.valueOf(rows.get(0).get("schema_name"));
		String sample = String.valueOf(rows.get(0).get("sample"));
		String plan = jdbcTemplate.queryForObject(
				"EXPLAIN FORMAT=TREE FOR SCHEMA `" + schema.replace("`", "``") + "` " + sample, String.class);

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("query", sample);
		result.put("plan", plan);
		return result;
	}

}
