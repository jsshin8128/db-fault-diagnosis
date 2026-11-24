package io.github.jsshin8128.diagnosis.strategy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.github.jsshin8128.diagnosis.Diagnosis;

public record Assessment(
		Diagnosis diagnosis,
		double confidence,
		Map<String, Double> expectedGains
) {

	public static Map<String, Object> schema(List<String> specialistNames) {
		Map<String, Object> gains = new LinkedHashMap<>();
		specialistNames.forEach(name -> gains.put(name, Map.of("type", "number", "description", "0 to 1")));
		return Map.of(
				"type", "object",
				"properties", Map.of(
						"diagnosis", Diagnosis.SCHEMA,
						"confidence", Map.of("type", "number", "description", "0 to 1"),
						"expectedGains", Map.of(
								"type", "object",
								"properties", gains,
								"required", specialistNames,
								"additionalProperties", false)),
				"required", List.of("diagnosis", "confidence", "expectedGains"),
				"additionalProperties", false);
	}

}
