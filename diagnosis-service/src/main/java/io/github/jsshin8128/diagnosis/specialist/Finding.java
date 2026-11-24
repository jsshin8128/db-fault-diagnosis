package io.github.jsshin8128.diagnosis.specialist;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.github.jsshin8128.diagnosis.RootCause;

public record Finding(
		RootCause suspectedCause,
		String targetTable,
		String targetColumn,
		String evidence
) {

	public static final Map<String, Object> SCHEMA = Map.of(
			"type", "object",
			"properties", Map.of(
					"suspectedCause", Map.of(
							"type", "string",
							"enum", Arrays.stream(RootCause.values()).map(Enum::name).toList()),
					"targetTable", Map.of("type", List.of("string", "null")),
					"targetColumn", Map.of("type", List.of("string", "null")),
					"evidence", Map.of("type", "string")),
			"required", List.of("suspectedCause", "targetTable", "targetColumn", "evidence"),
			"additionalProperties", false);

}
