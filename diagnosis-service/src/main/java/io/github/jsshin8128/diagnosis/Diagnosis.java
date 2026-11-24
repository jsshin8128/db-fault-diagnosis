package io.github.jsshin8128.diagnosis;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public record Diagnosis(
		RootCause rootCause,
		String targetTable,
		String targetColumn,
		String evidence,
		String recommendedAction
) {

	public static final Map<String, Object> SCHEMA = Map.of(
			"type", "object",
			"properties", Map.of(
					"rootCause", Map.of(
							"type", "string",
							"enum", Arrays.stream(RootCause.values()).map(Enum::name).toList()),
					"targetTable", Map.of("type", List.of("string", "null")),
					"targetColumn", Map.of("type", List.of("string", "null")),
					"evidence", Map.of("type", "string"),
					"recommendedAction", Map.of("type", "string")),
			"required", List.of("rootCause", "targetTable", "targetColumn", "evidence", "recommendedAction"),
			"additionalProperties", false);

}
