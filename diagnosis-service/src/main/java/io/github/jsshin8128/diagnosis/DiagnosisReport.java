package io.github.jsshin8128.diagnosis;

import java.util.List;

import io.github.jsshin8128.diagnosis.agent.AgentUsage;
import io.github.jsshin8128.diagnosis.agent.ToolCall;

public record DiagnosisReport(
		String strategy,
		Diagnosis diagnosis,
		long inputTokens,
		long outputTokens,
		long cacheReadInputTokens,
		int requestCount,
		long durationMs,
		List<ToolCall> toolCalls,
		List<AgentUsage> agentUsages,
		List<Selection> selections
) {

	public static DiagnosisReport of(String strategy, Diagnosis diagnosis, long durationMs,
			List<AgentUsage> agentUsages, List<Selection> selections) {
		return new DiagnosisReport(
				strategy,
				diagnosis,
				agentUsages.stream().mapToLong(AgentUsage::inputTokens).sum(),
				agentUsages.stream().mapToLong(AgentUsage::outputTokens).sum(),
				agentUsages.stream().mapToLong(AgentUsage::cacheReadInputTokens).sum(),
				agentUsages.stream().mapToInt(AgentUsage::requestCount).sum(),
				durationMs,
				agentUsages.stream().flatMap(usage -> usage.toolCalls().stream()).toList(),
				agentUsages,
				selections);
	}

}
