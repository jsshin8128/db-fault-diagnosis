package io.github.jsshin8128.diagnosis.tool;

import java.util.Map;

public interface DiagnosticTool {

	String name();

	String description();

	InputSchema inputSchema();

	Object execute(Map<String, Object> input);

}
