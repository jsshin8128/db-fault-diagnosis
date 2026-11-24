package io.github.jsshin8128.diagnosis;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "diagnosis")
public record DiagnosisProperties(
		String targetBaseUrl,
		String model,
		String effort,
		long maxTokens
) {
}
