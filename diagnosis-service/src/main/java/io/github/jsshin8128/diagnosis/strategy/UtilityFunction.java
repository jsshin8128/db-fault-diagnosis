package io.github.jsshin8128.diagnosis.strategy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.github.jsshin8128.diagnosis.Selection;
import org.springframework.stereotype.Component;

@Component
public class UtilityFunction implements SelectionPolicy {

	private final ProgressiveProperties properties;
	private final double totalTokens;
	private final double totalLatencyMs;

	public UtilityFunction(ProgressiveProperties properties) {
		this.properties = properties;
		this.totalTokens = properties.costs().values().stream().mapToDouble(ProgressiveProperties.Cost::tokens).sum();
		this.totalLatencyMs = properties.costs().values().stream().mapToDouble(ProgressiveProperties.Cost::latencyMs).sum();
	}

	public double utility(double confidence, double expectedGain, String specialist) {
		ProgressiveProperties.Cost cost = properties.costs().get(specialist);
		if (cost == null) {
			throw new IllegalStateException("No cost configured for specialist " + specialist);
		}
		double qualityGain = (1 - confidence) * expectedGain;
		return qualityGain
				- properties.alpha() * cost.tokens() / totalTokens
				- properties.beta() * cost.latencyMs() / totalLatencyMs;
	}

	@Override
	public Selection select(double confidence, Map<String, Double> expectedGains, List<String> candidates) {
		double boundedConfidence = bound(confidence);
		Map<String, Double> gains = new LinkedHashMap<>();
		Map<String, Double> utilities = new LinkedHashMap<>();
		String best = null;
		for (String candidate : candidates) {
			double gain = bound(expectedGains.getOrDefault(candidate, 0.0));
			double utility = utility(boundedConfidence, gain, candidate);
			gains.put(candidate, gain);
			utilities.put(candidate, utility);
			if (best == null || utility > utilities.get(best)) {
				best = candidate;
			}
		}
		boolean stop = best == null
				|| boundedConfidence >= properties.confidenceThreshold()
				|| utilities.get(best) <= 0;
		return new Selection(boundedConfidence, gains, utilities, stop ? null : best);
	}

	private static double bound(double value) {
		return Math.max(0.0, Math.min(1.0, value));
	}

}
