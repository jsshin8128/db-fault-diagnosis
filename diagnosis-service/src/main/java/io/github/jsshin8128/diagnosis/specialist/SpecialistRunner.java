package io.github.jsshin8128.diagnosis.specialist;

import java.util.List;
import java.util.stream.Collectors;

import io.github.jsshin8128.diagnosis.DiagnosisRequest;
import io.github.jsshin8128.diagnosis.agent.AgentLoop;
import io.github.jsshin8128.diagnosis.agent.AgentRun;
import io.github.jsshin8128.diagnosis.agent.AgentSpec;
import io.github.jsshin8128.diagnosis.agent.AgentUsage;
import org.springframework.stereotype.Component;

@Component
public class SpecialistRunner {

	private final AgentLoop agentLoop;

	public SpecialistRunner(AgentLoop agentLoop) {
		this.agentLoop = agentLoop;
	}

	public SpecialistRun run(Specialist specialist, DiagnosisRequest request, List<SpecialistRun> priorRuns) {
		AgentRun<Finding> run = agentLoop.run(
				new AgentSpec<>(specialist.systemPrompt(), specialist.tools(), Finding.SCHEMA, Finding.class),
				message(request, priorRuns));
		return new SpecialistRun(specialist, run.output(), AgentUsage.of(specialist.name(), run));
	}

	private static String message(DiagnosisRequest request, List<SpecialistRun> priorRuns) {
		String findings = priorRuns.isEmpty()
				? "None yet."
				: priorRuns.stream().map(SpecialistRun::describe).collect(Collectors.joining("\n\n"));
		return "The endpoint " + request.method() + " " + request.path() + " is reported as slow.\n\n"
				+ "Findings from other analysts so far:\n" + findings + "\n\n"
				+ "Investigate your area and report your finding.";
	}

}
