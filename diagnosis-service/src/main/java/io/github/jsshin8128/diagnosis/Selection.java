package io.github.jsshin8128.diagnosis;

import java.util.Map;

public record Selection(
		double confidence,
		Map<String, Double> expectedGains,
		Map<String, Double> utilities,
		String selected
) {
}
