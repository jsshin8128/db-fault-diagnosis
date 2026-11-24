package io.github.jsshin8128.diagnosis.strategy;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "diagnosis.progressive")
public record ProgressiveProperties(
		double alpha,
		double beta,
		double confidenceThreshold,
		Map<String, Cost> costs
) {

	public record Cost(
			double tokens,
			double latencyMs
	) {
	}

}
