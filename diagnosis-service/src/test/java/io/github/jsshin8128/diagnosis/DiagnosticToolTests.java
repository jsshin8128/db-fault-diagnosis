package io.github.jsshin8128.diagnosis;

import java.util.List;
import java.util.Map;

import io.github.jsshin8128.diagnosis.tool.ApiLatencyTool;
import io.github.jsshin8128.diagnosis.tool.ConnectionPoolTool;
import io.github.jsshin8128.diagnosis.tool.LockWaitTool;
import io.github.jsshin8128.diagnosis.tool.QueryPlanTool;
import io.github.jsshin8128.diagnosis.tool.SlowQueryTool;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DiagnosticToolTests {

	@Autowired
	private ApiLatencyTool apiLatencyTool;

	@Autowired
	private SlowQueryTool slowQueryTool;

	@Autowired
	private QueryPlanTool queryPlanTool;

	@Autowired
	private ConnectionPoolTool connectionPoolTool;

	@Autowired
	private LockWaitTool lockWaitTool;

	@Test
	@SuppressWarnings("unchecked")
	void toolsReadFromRunningTarget() {
		Map<String, Object> latency = (Map<String, Object>) apiLatencyTool.execute(Map.of("method", "GET", "path", "/api/orders"));
		System.out.println("LATENCY " + latency);
		assertThat(latency).containsKey("count");

		Map<String, Object> templatedPathLatency = (Map<String, Object>) apiLatencyTool
				.execute(Map.of("method", "PATCH", "path", "/api/orders/{id}/status"));
		System.out.println("TEMPLATED " + templatedPathLatency);
		assertThat(templatedPathLatency).containsKey("count");

		List<Map<String, Object>> queries = (List<Map<String, Object>>) slowQueryTool.execute(Map.of("limit", 20));
		Map<String, Object> select = queries.stream()
				.filter(query -> String.valueOf(query.get("query")).startsWith("SELECT `id`"))
				.findFirst()
				.orElseThrow();
		System.out.println("QUERY " + select);

		Map<String, Object> plan = (Map<String, Object>) queryPlanTool.execute(Map.of("digest", select.get("digest")));
		System.out.println("PLAN " + plan);
		assertThat(plan).containsKey("plan");

		Map<String, Object> pool = (Map<String, Object>) connectionPoolTool.execute(Map.of());
		System.out.println("POOL " + pool);
		assertThat(pool.get("max")).isNotNull();

		Map<String, Object> lockWaits = (Map<String, Object>) lockWaitTool.execute(Map.of());
		System.out.println("LOCKS " + lockWaits);
		assertThat(lockWaits).containsKey("wait_count");
	}

}
