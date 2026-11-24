package io.github.jsshin8128.diagnosis.strategy;

import io.github.jsshin8128.diagnosis.DiagnosisReport;
import io.github.jsshin8128.diagnosis.DiagnosisRequest;

public interface DiagnosisStrategy {

	String name();

	DiagnosisReport diagnose(DiagnosisRequest request);

}
