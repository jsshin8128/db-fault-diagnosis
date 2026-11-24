package io.github.jsshin8128.diagnosis.strategy;

import java.util.List;

import io.github.jsshin8128.diagnosis.agent.AgentLoop;
import io.github.jsshin8128.diagnosis.specialist.Specialist;
import io.github.jsshin8128.diagnosis.specialist.SpecialistRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StrategyConfig {

	@Bean
	public DiagnosisStrategy progressiveCollaborationStrategy(List<Specialist> specialists,
			SpecialistRunner specialistRunner, AgentLoop agentLoop, UtilityFunction utilityFunction) {
		specialists.forEach(specialist -> utilityFunction.utility(0, 0, specialist.name()));
		return new ProgressiveCollaborationStrategy("progressive-collaboration", specialists, specialistRunner,
				agentLoop, utilityFunction);
	}

	@Bean
	public DiagnosisStrategy fixedOrderCollaborationStrategy(List<Specialist> specialists,
			SpecialistRunner specialistRunner, AgentLoop agentLoop, FixedOrderSelection fixedOrderSelection) {
		return new ProgressiveCollaborationStrategy("fixed-order-collaboration", specialists, specialistRunner,
				agentLoop, fixedOrderSelection);
	}

}
