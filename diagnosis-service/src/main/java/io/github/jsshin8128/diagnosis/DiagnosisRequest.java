package io.github.jsshin8128.diagnosis;

public record DiagnosisRequest(
		String strategy,
		String method,
		String path
) {
}
