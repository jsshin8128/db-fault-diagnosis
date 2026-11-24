package io.github.jsshin8128.diagnosis.strategy;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import io.github.jsshin8128.diagnosis.Diagnosis;
import io.github.jsshin8128.diagnosis.DiagnosisReport;
import io.github.jsshin8128.diagnosis.DiagnosisRequest;
import io.github.jsshin8128.diagnosis.agent.AgentLoop;
import io.github.jsshin8128.diagnosis.agent.AgentRun;
import io.github.jsshin8128.diagnosis.agent.AgentSpec;
import io.github.jsshin8128.diagnosis.agent.AgentUsage;
import io.github.jsshin8128.diagnosis.specialist.Specialist;
import io.github.jsshin8128.diagnosis.specialist.SpecialistRun;
import io.github.jsshin8128.diagnosis.specialist.SpecialistRunner;
import org.springframework.stereotype.Component;

@Component
public class FullCollaborationStrategy implements DiagnosisStrategy {

	private static final String SYSTEM_PROMPT = """
			You lead a team diagnosing why an API endpoint is slow. The endpoint belongs to a Spring Boot service backed by a MySQL database.
			Each analyst investigated one area and reported a finding. Combine the findings into one diagnosis.
			An endpoint is considered slow when its p99 latency exceeds 500 ms.
			If the findings show that the endpoint is not slow, report NO_PROBLEM.
			If the findings are not enough to determine a cause, report UNDETERMINED.
			Report targetTable and targetColumn only when the cause is located in a specific table or column.""";

	private final List<Specialist> specialists;
	private final SpecialistRunner specialistRunner;
	private final AgentLoop agentLoop;

	public FullCollaborationStrategy(List<Specialist> specialists, SpecialistRunner specialistRunner, AgentLoop agentLoop) {
		this.specialists = specialists;
		this.specialistRunner = specialistRunner;
		this.agentLoop = agentLoop;
	}

	@Override
	public String name() {
		return "full-collaboration";
	}

	@Override
	public DiagnosisReport diagnose(DiagnosisRequest request) {
		long startedAt = System.nanoTime();
		List<SpecialistRun> runs = new ArrayList<>();
		for (Specialist specialist : specialists) {
			runs.add(specialistRunner.run(specialist, request, runs));
		}

		AgentRun<Diagnosis> synthesis = agentLoop.run(
				new AgentSpec<>(SYSTEM_PROMPT, List.of(), Diagnosis.SCHEMA, Diagnosis.class),
				"The endpoint " + request.method() + " " + request.path() + " is reported as slow.\n\n"
						+ "Findings:\n" + runs.stream().map(SpecialistRun::describe).collect(Collectors.joining("\n\n")));
		long durationMs = (System.nanoTime() - startedAt) / 1_000_000;

		List<AgentUsage> usages = new ArrayList<>(runs.stream().map(SpecialistRun::usage).toList());
		usages.add(AgentUsage.of("main_agent", synthesis));
		return DiagnosisReport.of(name(), synthesis.output(), durationMs, usages, List.of());
	}

}
