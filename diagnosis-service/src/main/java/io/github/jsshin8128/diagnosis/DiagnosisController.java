package io.github.jsshin8128.diagnosis;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.github.jsshin8128.diagnosis.strategy.DiagnosisStrategy;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/diagnoses")
public class DiagnosisController {

	private final Map<String, DiagnosisStrategy> strategies;

	public DiagnosisController(List<DiagnosisStrategy> strategies) {
		this.strategies = strategies.stream()
				.collect(Collectors.toMap(DiagnosisStrategy::name, Function.identity()));
	}

	@PostMapping
	public DiagnosisReport diagnose(@RequestBody DiagnosisRequest request) {
		DiagnosisStrategy strategy = strategies.get(request.strategy());
		if (strategy == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Unknown strategy: " + request.strategy() + ". Available: " + strategies.keySet());
		}
		return strategy.diagnose(request);
	}

}
