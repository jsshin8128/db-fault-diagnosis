package io.github.jsshin8128.diagnosis.agent;

import java.util.Map;

public record ToolCall(
		String name,
		Map<String, Object> input
) {
}
