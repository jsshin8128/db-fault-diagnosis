package io.github.jsshin8128.diagnosis;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import io.github.jsshin8128.diagnosis.strategy.ProgressiveProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({DiagnosisProperties.class, ProgressiveProperties.class})
public class DiagnosisConfig {

	@Bean
	@Lazy
	public AnthropicClient anthropicClient() {
		return AnthropicOkHttpClient.fromEnv();
	}

	@Bean
	public RestClient targetRestClient(DiagnosisProperties properties) {
		return RestClient.builder().baseUrl(properties.targetBaseUrl()).build();
	}

}
