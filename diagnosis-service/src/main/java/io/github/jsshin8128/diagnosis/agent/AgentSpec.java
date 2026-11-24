package io.github.jsshin8128.diagnosis.agent;

import java.util.List;
import java.util.Map;

import io.github.jsshin8128.diagnosis.tool.DiagnosticTool;

public record AgentSpec<T>(
		String systemPrompt,
		List<DiagnosticTool> tools,
		Map<String, Object> outputSchema,
		Class<T> outputType
) {
}
