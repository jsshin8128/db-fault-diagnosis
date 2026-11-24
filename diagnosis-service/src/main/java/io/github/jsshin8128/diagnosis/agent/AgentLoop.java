package io.github.jsshin8128.diagnosis.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.anthropic.client.AnthropicClient;
import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.JsonOutputFormat;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.Tool;
import com.anthropic.models.messages.ToolResultBlockParam;
import com.anthropic.models.messages.ToolUseBlock;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jsshin8128.diagnosis.DiagnosisProperties;
import io.github.jsshin8128.diagnosis.tool.DiagnosticTool;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class AgentLoop {

	private static final int MAX_REQUESTS = 20;
	private static final TypeReference<Map<String, Object>> INPUT_TYPE = new TypeReference<>() {
	};

	private final ObjectProvider<AnthropicClient> anthropicClient;
	private final DiagnosisProperties properties;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public AgentLoop(ObjectProvider<AnthropicClient> anthropicClient, DiagnosisProperties properties) {
		this.anthropicClient = anthropicClient;
		this.properties = properties;
	}

	public <T> AgentRun<T> run(AgentSpec<T> spec, String userMessage) {
		long startedAt = System.nanoTime();
		Map<String, DiagnosticTool> toolsByName = spec.tools().stream()
				.collect(Collectors.toMap(DiagnosticTool::name, Function.identity()));

		MessageCreateParams.Builder params = MessageCreateParams.builder()
				.model(properties.model())
				.maxTokens(properties.maxTokens())
				.system(spec.systemPrompt())
				.outputConfig(OutputConfig.builder()
						.effort(OutputConfig.Effort.of(properties.effort()))
						.format(JsonOutputFormat.builder()
								.schema(JsonOutputFormat.Schema.builder()
										.putAllAdditionalProperties(toJsonValues(spec.outputSchema()))
										.build())
								.build())
						.build())
				.addUserMessage(userMessage);
		spec.tools().forEach(tool -> params.addTool(toSdkTool(tool)));

		long inputTokens = 0;
		long outputTokens = 0;
		long cacheReadInputTokens = 0;
		List<ToolCall> toolCalls = new ArrayList<>();

		for (int requestCount = 1; requestCount <= MAX_REQUESTS; requestCount++) {
			Message message = anthropicClient.getObject().messages().create(params.build());
			inputTokens += message.usage().inputTokens();
			outputTokens += message.usage().outputTokens();
			cacheReadInputTokens += message.usage().cacheReadInputTokens().orElse(0L);

			StopReason stopReason = message.stopReason().orElse(null);
			if (StopReason.REFUSAL.equals(stopReason) || StopReason.MAX_TOKENS.equals(stopReason)) {
				throw new IllegalStateException("Model stopped with " + stopReason);
			}

			List<ToolUseBlock> toolUses = message.content().stream()
					.flatMap(block -> block.toolUse().stream())
					.toList();
			if (toolUses.isEmpty()) {
				String output = message.content().stream()
						.flatMap(block -> block.text().stream())
						.map(text -> text.text())
						.collect(Collectors.joining());
				long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
				return new AgentRun<>(parse(output, spec.outputType()), inputTokens, outputTokens,
						cacheReadInputTokens, requestCount, durationMs, toolCalls);
			}

			params.addMessage(message);
			List<ContentBlockParam> results = new ArrayList<>();
			for (ToolUseBlock toolUse : toolUses) {
				Map<String, Object> input = toolUse._input().convert(INPUT_TYPE);
				toolCalls.add(new ToolCall(toolUse.name(), input));
				results.add(ContentBlockParam.ofToolResult(execute(toolsByName.get(toolUse.name()), toolUse, input)));
			}
			params.addUserMessageOfContentBlockParams(results);
		}
		throw new IllegalStateException("Agent did not finish within " + MAX_REQUESTS + " requests");
	}

	private <T> T parse(String output, Class<T> outputType) {
		try {
			return objectMapper.readValue(output, outputType);
		}
		catch (JsonProcessingException e) {
			throw new IllegalStateException("Model output does not match " + outputType.getSimpleName() + ": " + output, e);
		}
	}

	private ToolResultBlockParam execute(DiagnosticTool tool, ToolUseBlock toolUse, Map<String, Object> input) {
		ToolResultBlockParam.Builder result = ToolResultBlockParam.builder().toolUseId(toolUse.id());
		if (tool == null) {
			return result.content("Unknown tool: " + toolUse.name()).isError(true).build();
		}
		try {
			return result.content(objectMapper.writeValueAsString(tool.execute(input))).build();
		}
		catch (JsonProcessingException | RuntimeException e) {
			return result.content(String.valueOf(e.getMessage())).isError(true).build();
		}
	}

	private static Tool toSdkTool(DiagnosticTool tool) {
		Tool.InputSchema.Properties.Builder properties = Tool.InputSchema.Properties.builder();
		tool.inputSchema().properties().forEach((name, schema) -> properties.putAdditionalProperty(name, JsonValue.from(schema)));
		return Tool.builder()
				.name(tool.name())
				.description(tool.description())
				.inputSchema(Tool.InputSchema.builder()
						.properties(properties.build())
						.required(tool.inputSchema().required())
						.build())
				.build();
	}

	private static Map<String, JsonValue> toJsonValues(Map<String, Object> values) {
		return values.entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, entry -> JsonValue.from(entry.getValue())));
	}

}
