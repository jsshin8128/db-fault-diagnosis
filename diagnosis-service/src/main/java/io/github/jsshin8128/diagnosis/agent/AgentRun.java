package io.github.jsshin8128.diagnosis.agent;

import java.util.List;

public record AgentRun<T>(
		T output,
		long inputTokens,
		long outputTokens,
		long cacheReadInputTokens,
		int requestCount,
		long durationMs,
		List<ToolCall> toolCalls
) {
}
