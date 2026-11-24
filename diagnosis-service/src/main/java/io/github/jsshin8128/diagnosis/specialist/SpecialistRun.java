package io.github.jsshin8128.diagnosis.specialist;

import io.github.jsshin8128.diagnosis.agent.AgentUsage;

public record SpecialistRun(
		Specialist specialist,
		Finding finding,
		AgentUsage usage
) {

	public String describe() {
		return "[" + specialist.name() + "] suspectedCause=" + finding.suspectedCause()
				+ ", targetTable=" + finding.targetTable()
				+ ", targetColumn=" + finding.targetColumn()
				+ "\nevidence: " + finding.evidence();
	}

}
