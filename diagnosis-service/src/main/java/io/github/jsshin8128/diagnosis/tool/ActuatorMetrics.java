package io.github.jsshin8128.diagnosis.tool;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
public class ActuatorMetrics {

	private final RestClient targetRestClient;

	public ActuatorMetrics(RestClient targetRestClient) {
		this.targetRestClient = targetRestClient;
	}

	@SuppressWarnings("unchecked")
	public Map<String, Double> measurements(String metric, List<String> tags) {
		Map<String, Object> body;
		try {
			body = targetRestClient.get()
					.uri(uriBuilder -> {
						uriBuilder.path("/actuator/metrics/" + metric);
						Map<String, String> variables = new LinkedHashMap<>();
						for (int i = 0; i < tags.size(); i++) {
							uriBuilder.queryParam("tag", "{tag" + i + "}");
							variables.put("tag" + i, tags.get(i));
						}
						return uriBuilder.build(variables);
					})
					.retrieve()
					.body(Map.class);
		}
		catch (HttpClientErrorException.NotFound e) {
			return Map.of();
		}
		Map<String, Double> values = new LinkedHashMap<>();
		if (body == null) {
			return values;
		}
		for (Map<String, Object> measurement : (List<Map<String, Object>>) body.get("measurements")) {
			values.put(String.valueOf(measurement.get("statistic")), ((Number) measurement.get("value")).doubleValue());
		}
		return values;
	}

	public static double toMillis(double seconds) {
		return Math.round(seconds * 1000.0 * 100.0) / 100.0;
	}

}
