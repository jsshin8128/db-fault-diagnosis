package io.github.jsshin8128.diagnosis.specialist;

import java.util.List;

import io.github.jsshin8128.diagnosis.tool.DiagnosticTool;

public record Specialist(
		String name,
		String description,
		String systemPrompt,
		List<DiagnosticTool> tools
) {
}
