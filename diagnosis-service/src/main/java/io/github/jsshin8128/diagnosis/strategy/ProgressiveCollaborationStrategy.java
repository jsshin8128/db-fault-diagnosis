package io.github.jsshin8128.diagnosis.strategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.github.jsshin8128.diagnosis.DiagnosisReport;
import io.github.jsshin8128.diagnosis.DiagnosisRequest;
import io.github.jsshin8128.diagnosis.Selection;
import io.github.jsshin8128.diagnosis.agent.AgentLoop;
import io.github.jsshin8128.diagnosis.agent.AgentRun;
import io.github.jsshin8128.diagnosis.agent.AgentSpec;
import io.github.jsshin8128.diagnosis.agent.AgentUsage;
import io.github.jsshin8128.diagnosis.specialist.Specialist;
import io.github.jsshin8128.diagnosis.specialist.SpecialistRun;
import io.github.jsshin8128.diagnosis.specialist.SpecialistRunner;
public class ProgressiveCollaborationStrategy implements DiagnosisStrategy {

	private static final String SYSTEM_PROMPT = """
			You lead a team diagnosing why an API endpoint is slow. The endpoint belongs to a Spring Boot service backed by a MySQL database.
			Analysts are consulted one at a time, and each reports a finding for one area. After each finding you assess the case.
			An endpoint is considered slow when its p99 latency exceeds 500 ms.
			Give your current best diagnosis from the findings so far. If they show that the endpoint is not slow, report NO_PROBLEM.
			If they are not enough to determine a cause, report UNDETERMINED.
			Report targetTable and targetColumn only when the cause is located in a specific table or column.
			confidence is how sure you are, from 0 to 1, that this diagnosis is correct and needs no further analyst.
			expectedGains gives, for each analyst, how likely from 0 to 1 it is that consulting them would change or complete the diagnosis.
			Give 0 to analysts who have already reported.""";

	private final String name;
	private final List<Specialist> specialists;
	private final SpecialistRunner specialistRunner;
	private final AgentLoop agentLoop;
	private final SelectionPolicy selectionPolicy;
	private final Map<String, Object> assessmentSchema;

	public ProgressiveCollaborationStrategy(String name, List<Specialist> specialists, SpecialistRunner specialistRunner,
			AgentLoop agentLoop, SelectionPolicy selectionPolicy) {
		this.name = name;
		this.specialists = specialists;
		this.specialistRunner = specialistRunner;
		this.agentLoop = agentLoop;
		this.selectionPolicy = selectionPolicy;
		this.assessmentSchema = Assessment.schema(specialists.stream().map(Specialist::name).toList());
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public DiagnosisReport diagnose(DiagnosisRequest request) {
		long startedAt = System.nanoTime();
		List<Specialist> remaining = new ArrayList<>(specialists);
		List<SpecialistRun> runs = new ArrayList<>();
		List<AgentUsage> usages = new ArrayList<>();
		List<Selection> selections = new ArrayList<>();

		Specialist next = remaining.get(0);
		Assessment assessment;
		while (true) {
			remaining.remove(next);
			SpecialistRun run = specialistRunner.run(next, request, runs);
			runs.add(run);
			usages.add(run.usage());

			AgentRun<Assessment> assessed = assess(request, runs, remaining);
			usages.add(AgentUsage.of("main_agent", assessed));
			assessment = assessed.output();

			Selection selection = selectionPolicy.select(assessment.confidence(), assessment.expectedGains(),
					remaining.stream().map(Specialist::name).toList());
			selections.add(selection);
			if (selection.selected() == null) {
				break;
			}
			next = remaining.stream()
					.filter(specialist -> specialist.name().equals(selection.selected()))
					.findFirst()
					.orElseThrow();
		}

		long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
		return DiagnosisReport.of(name(), assessment.diagnosis(), durationMs, usages, selections);
	}

	private AgentRun<Assessment> assess(DiagnosisRequest request, List<SpecialistRun> runs, List<Specialist> remaining) {
		String notConsulted = remaining.isEmpty()
				? "None."
				: remaining.stream()
						.map(specialist -> specialist.name() + ": " + specialist.description())
						.collect(Collectors.joining("\n"));
		return agentLoop.run(
				new AgentSpec<>(SYSTEM_PROMPT, List.of(), assessmentSchema, Assessment.class),
				"The endpoint " + request.method() + " " + request.path() + " is reported as slow.\n\n"
						+ "Findings so far:\n" + runs.stream().map(SpecialistRun::describe).collect(Collectors.joining("\n\n"))
						+ "\n\nAnalysts not yet consulted:\n" + notConsulted);
	}

}
