package io.github.jsshin8128.diagnosis.tool;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class ConnectionPoolTool implements DiagnosticTool {

	private static final String PREFIX = "hikaricp.connections";

	private final ActuatorMetrics actuatorMetrics;

	public ConnectionPoolTool(ActuatorMetrics actuatorMetrics) {
		this.actuatorMetrics = actuatorMetrics;
	}

	@Override
	public String name() {
		return "get_connection_pool";
	}

	@Override
	public String description() {
		return "Returns the state of the target service's database connection pool: "
				+ "the maximum pool size, active and idle connections, threads waiting for a connection, "
				+ "the number of connection acquisition timeouts, and the mean and max time to acquire a connection in milliseconds.";
	}

	@Override
	public InputSchema inputSchema() {
		return new InputSchema(Map.of(), List.of());
	}

	@Override
	public Object execute(Map<String, Object> input) {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("max", gauge(".max"));
		result.put("active", gauge(".active"));
		result.put("idle", gauge(".idle"));
		result.put("pending", gauge(".pending"));
		result.put("timeout_count", actuatorMetrics.measurements(PREFIX + ".timeout", List.of()).get("COUNT"));

		Map<String, Double> acquire = actuatorMetrics.measurements(PREFIX + ".acquire", List.of());
		double count = acquire.getOrDefault("COUNT", 0.0);
		result.put("acquire_count", (long) count);
		if (count > 0) {
			result.put("acquire_mean_ms", ActuatorMetrics.toMillis(acquire.getOrDefault("TOTAL_TIME", 0.0) / count));
			result.put("acquire_max_ms", ActuatorMetrics.toMillis(acquire.getOrDefault("MAX", 0.0)));
		}
		return result;
	}

	private Double gauge(String suffix) {
		return actuatorMetrics.measurements(PREFIX + suffix, List.of()).get("VALUE");
	}

}
