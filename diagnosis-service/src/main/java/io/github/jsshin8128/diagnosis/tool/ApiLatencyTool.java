package io.github.jsshin8128.diagnosis.tool;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class ApiLatencyTool implements DiagnosticTool {

	private static final String METRIC = "http.server.requests";
	private static final Map<String, String> PERCENTILES = Map.of("p50_ms", "0.5", "p95_ms", "0.95", "p99_ms", "0.99");

	private final ActuatorMetrics actuatorMetrics;

	public ApiLatencyTool(ActuatorMetrics actuatorMetrics) {
		this.actuatorMetrics = actuatorMetrics;
	}

	@Override
	public String name() {
		return "get_api_latency";
	}

	@Override
	public String description() {
		return "Returns latency statistics of one API endpoint of the target service: "
				+ "request count, mean, max, and the p50, p95, p99 latency in milliseconds. "
				+ "A count of 0 means the endpoint has not been called since the service started.";
	}

	@Override
	public InputSchema inputSchema() {
		return new InputSchema(
				Map.of(
						"method", Map.of("type", "string", "description", "HTTP method, e.g. GET"),
						"path", Map.of("type", "string", "description", "Request path without query string, e.g. /api/orders")
				),
				List.of("method", "path")
		);
	}

	@Override
	public Object execute(Map<String, Object> input) {
		List<String> tags = List.of(
				"uri:" + input.get("path"),
				"method:" + String.valueOf(input.get("method")).toUpperCase());

		Map<String, Object> result = new LinkedHashMap<>();
		Map<String, Double> timer = actuatorMetrics.measurements(METRIC, tags);
		double count = timer.getOrDefault("COUNT", 0.0);
		result.put("count", (long) count);
		if (count == 0) {
			return result;
		}
		result.put("mean_ms", ActuatorMetrics.toMillis(timer.getOrDefault("TOTAL_TIME", 0.0) / count));
		result.put("max_ms", ActuatorMetrics.toMillis(timer.getOrDefault("MAX", 0.0)));
		for (String key : List.of("p50_ms", "p95_ms", "p99_ms")) {
			List<String> percentileTags = new ArrayList<>(tags);
			percentileTags.add("phi:" + PERCENTILES.get(key));
			Double value = actuatorMetrics.measurements(METRIC + ".percentile", percentileTags).get("VALUE");
			result.put(key, value == null ? null : ActuatorMetrics.toMillis(value));
		}
		return result;
	}

}
