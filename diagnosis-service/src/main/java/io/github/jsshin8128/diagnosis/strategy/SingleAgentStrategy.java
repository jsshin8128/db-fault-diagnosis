package io.github.jsshin8128.diagnosis.strategy;

import java.util.List;

import io.github.jsshin8128.diagnosis.Diagnosis;
import io.github.jsshin8128.diagnosis.DiagnosisReport;
import io.github.jsshin8128.diagnosis.DiagnosisRequest;
import io.github.jsshin8128.diagnosis.agent.AgentLoop;
import io.github.jsshin8128.diagnosis.agent.AgentRun;
import io.github.jsshin8128.diagnosis.agent.AgentSpec;
import io.github.jsshin8128.diagnosis.agent.AgentUsage;
import io.github.jsshin8128.diagnosis.tool.DiagnosticTool;
import org.springframework.stereotype.Component;

@Component
public class SingleAgentStrategy implements DiagnosisStrategy {

	private static final String SYSTEM_PROMPT = """
			You diagnose why an API endpoint is slow. The endpoint belongs to a Spring Boot service backed by a MySQL database.
			Use the available tools to gather evidence and determine the root cause. Call only the tools you need.
			An endpoint is considered slow when its p99 latency exceeds 500 ms.
			If the measurements show that the endpoint is not slow, report NO_PROBLEM.
			If the evidence is not enough to determine a cause, report UNDETERMINED.
			Report targetTable and targetColumn only when the cause is located in a specific table or column.""";

	private final AgentLoop agentLoop;
	private final List<DiagnosticTool> tools;

	public SingleAgentStrategy(AgentLoop agentLoop, List<DiagnosticTool> tools) {
		this.agentLoop = agentLoop;
		this.tools = tools;
	}

	@Override
	public String name() {
		return "single-agent";
	}

	@Override
	public DiagnosisReport diagnose(DiagnosisRequest request) {
		long startedAt = System.nanoTime();
		AgentRun<Diagnosis> run = agentLoop.run(
				new AgentSpec<>(SYSTEM_PROMPT, tools, Diagnosis.SCHEMA, Diagnosis.class),
				"The endpoint " + request.method() + " " + request.path() + " is reported as slow. Diagnose the cause.");
		long durationMs = (System.nanoTime() - startedAt) / 1_000_000;

		return DiagnosisReport.of(name(), run.output(), durationMs, List.of(AgentUsage.of("main_agent", run)), List.of());
	}

}
