package io.github.jsshin8128.diagnosis.tool;

import java.util.List;
import java.util.Map;

public record InputSchema(
		Map<String, Object> properties,
		List<String> required
) {
}
