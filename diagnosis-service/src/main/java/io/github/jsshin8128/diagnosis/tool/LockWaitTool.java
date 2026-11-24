package io.github.jsshin8128.diagnosis.tool;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class LockWaitTool implements DiagnosticTool {

	private static final String FIND_LOCK_WAITS = """
			SELECT waiting_lock.OBJECT_SCHEMA AS schema_name,
			       waiting_lock.OBJECT_NAME AS table_name,
			       waiting_lock.INDEX_NAME AS index_name,
			       waiting_lock.LOCK_TYPE AS lock_type,
			       waiting_lock.LOCK_MODE AS waiting_lock_mode,
			       blocking_lock.LOCK_MODE AS blocking_lock_mode,
			       waiting_statement.SQL_TEXT AS waiting_query,
			       ROUND(waiting_statement.TIMER_WAIT / 1000000000) AS waiting_ms,
			       blocking_statement.SQL_TEXT AS blocking_last_query
			FROM performance_schema.data_lock_waits lock_wait
			JOIN performance_schema.data_locks waiting_lock
			  ON waiting_lock.ENGINE_LOCK_ID = lock_wait.REQUESTING_ENGINE_LOCK_ID
			JOIN performance_schema.data_locks blocking_lock
			  ON blocking_lock.ENGINE_LOCK_ID = lock_wait.BLOCKING_ENGINE_LOCK_ID
			LEFT JOIN performance_schema.events_statements_current waiting_statement
			  ON waiting_statement.THREAD_ID = lock_wait.REQUESTING_THREAD_ID
			LEFT JOIN performance_schema.events_statements_current blocking_statement
			  ON blocking_statement.THREAD_ID = lock_wait.BLOCKING_THREAD_ID
			ORDER BY waiting_statement.TIMER_WAIT DESC
			LIMIT 20""";

	private final JdbcTemplate jdbcTemplate;

	public LockWaitTool(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public String name() {
		return "get_lock_waits";
	}

	@Override
	public String description() {
		return "Returns the lock waits happening in the database right now: for each waiting statement, "
				+ "the table and index it waits on, the lock modes of the waiting and the blocking session, "
				+ "how long it has been waiting in milliseconds, and the last statement of the blocking session. "
				+ "Only waits in progress at the moment of the call are visible; an empty list means no statement is waiting for a lock now.";
	}

	@Override
	public InputSchema inputSchema() {
		return new InputSchema(Map.of(), List.of());
	}

	@Override
	public Object execute(Map<String, Object> input) {
		List<Map<String, Object>> waits = jdbcTemplate.queryForList(FIND_LOCK_WAITS);
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("wait_count", waits.size());
		result.put("waits", waits);
		return result;
	}

}
