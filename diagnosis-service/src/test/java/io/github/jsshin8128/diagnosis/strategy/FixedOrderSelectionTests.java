package io.github.jsshin8128.diagnosis.strategy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FixedOrderSelectionTests {

	private static final FixedOrderSelection SELECTION =
			new FixedOrderSelection(new ProgressiveProperties(0.1, 0.1, 0.8, Map.of()));

	@Test
	void selectsTheNextCandidateInOrderRegardlessOfExpectedGains() {
		assertThat(SELECTION.select(0.3, Map.of("first", 0.0, "second", 1.0), List.of("first", "second")).selected())
				.isEqualTo("first");
	}

	@Test
	void stopsWhenConfidenceReachesTheThreshold() {
		assertThat(SELECTION.select(0.8, Map.of("first", 1.0), List.of("first")).selected()).isNull();
	}

}
