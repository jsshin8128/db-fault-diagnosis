package io.github.jsshin8128.diagnosis.strategy;

import java.util.List;
import java.util.Map;

import io.github.jsshin8128.diagnosis.Selection;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class UtilityFunctionTests {

	private static UtilityFunction utilityFunction(double alpha, double beta, double confidenceThreshold) {
		return new UtilityFunction(new ProgressiveProperties(alpha, beta, confidenceThreshold, Map.of(
				"cheap", new ProgressiveProperties.Cost(1000, 2000),
				"costly", new ProgressiveProperties.Cost(3000, 6000))));
	}

	@Test
	void utilityIsQualityGainMinusWeightedCostShares() {
		double utility = utilityFunction(0.2, 0.4, 0.8).utility(0.5, 0.6, "costly");

		assertThat(utility).isCloseTo((1 - 0.5) * 0.6 - 0.2 * 0.75 - 0.4 * 0.75, within(1e-9));
	}

	@Test
	void selectsTheCandidateWithTheHighestUtility() {
		Selection selection = utilityFunction(0.1, 0.1, 0.8)
				.select(0.3, Map.of("cheap", 0.2, "costly", 0.9), List.of("cheap", "costly"));

		assertThat(selection.selected()).isEqualTo("costly");
	}

	@Test
	void stopsWhenConfidenceReachesTheThreshold() {
		Selection selection = utilityFunction(0.1, 0.1, 0.8)
				.select(0.8, Map.of("cheap", 1.0, "costly", 1.0), List.of("cheap", "costly"));

		assertThat(selection.selected()).isNull();
	}

	@Test
	void stopsWhenNoCandidateHasPositiveUtility() {
		Selection selection = utilityFunction(0.5, 0.5, 0.8)
				.select(0.5, Map.of("cheap", 0.1, "costly", 0.1), List.of("cheap", "costly"));

		assertThat(selection.selected()).isNull();
	}

}
