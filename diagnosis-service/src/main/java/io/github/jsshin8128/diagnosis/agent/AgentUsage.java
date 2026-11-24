package io.github.jsshin8128.diagnosis.agent;

import java.util.List;

public record AgentUsage(
		String agent,
		long inputTokens,
		long outputTokens,
		long cacheReadInputTokens,
		int requestCount,
		long durationMs,
		List<ToolCall> toolCalls
) {

	public static AgentUsage of(String agent, AgentRun<?> run) {
		return new AgentUsage(
				agent,
				run.inputTokens(),
				run.outputTokens(),
				run.cacheReadInputTokens(),
				run.requestCount(),
				run.durationMs(),
				run.toolCalls());
	}

}
