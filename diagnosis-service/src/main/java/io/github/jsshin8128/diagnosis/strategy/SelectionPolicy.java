package io.github.jsshin8128.diagnosis.strategy;

import java.util.List;
import java.util.Map;

import io.github.jsshin8128.diagnosis.Selection;

public interface SelectionPolicy {

	Selection select(double confidence, Map<String, Double> expectedGains, List<String> candidates);

}
