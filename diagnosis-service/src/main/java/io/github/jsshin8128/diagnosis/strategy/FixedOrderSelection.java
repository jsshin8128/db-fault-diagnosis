package io.github.jsshin8128.diagnosis.strategy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.github.jsshin8128.diagnosis.Selection;
import org.springframework.stereotype.Component;

@Component
public class FixedOrderSelection implements SelectionPolicy {

	private final ProgressiveProperties properties;

	public FixedOrderSelection(ProgressiveProperties properties) {
		this.properties = properties;
	}

	@Override
	public Selection select(double confidence, Map<String, Double> expectedGains, List<String> candidates) {
		double boundedConfidence = Math.max(0.0, Math.min(1.0, confidence));
		Map<String, Double> gains = new LinkedHashMap<>();
		candidates.forEach(candidate -> gains.put(candidate, expectedGains.getOrDefault(candidate, 0.0)));
		boolean stop = candidates.isEmpty() || boundedConfidence >= properties.confidenceThreshold();
		return new Selection(boundedConfidence, gains, Map.of(), stop ? null : candidates.get(0));
	}

}
