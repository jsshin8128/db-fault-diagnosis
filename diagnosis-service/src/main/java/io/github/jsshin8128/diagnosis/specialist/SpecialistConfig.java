package io.github.jsshin8128.diagnosis.specialist;

import java.util.List;

import io.github.jsshin8128.diagnosis.tool.ApiLatencyTool;
import io.github.jsshin8128.diagnosis.tool.ConnectionPoolTool;
import io.github.jsshin8128.diagnosis.tool.LockWaitTool;
import io.github.jsshin8128.diagnosis.tool.QueryPlanTool;
import io.github.jsshin8128.diagnosis.tool.SlowQueryTool;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
public class SpecialistConfig {

	private static final String COMMON_RULES = """

			You are one analyst in a team diagnosing why an API endpoint of a Spring Boot service backed by MySQL is slow.
			An endpoint is considered slow when its p99 latency exceeds 500 ms.
			Investigate only your own area with your tools, and report only what your evidence supports.
			Report NO_PROBLEM when your area shows nothing wrong, and UNDETERMINED when your evidence cannot tell.
			Report targetTable and targetColumn only when the cause is located in a specific table or column.
			Keep the evidence to a few sentences, and quote the identifiers another analyst would need, such as a statement digest.""";

	@Bean
	@Order(1)
	public Specialist apiAnalysis(ApiLatencyTool apiLatencyTool) {
		return new Specialist(
				"api_analysis",
				"Measures the latency of the reported endpoint and tells whether it is actually slow.",
				"Your area is the latency of the reported API endpoint. Determine whether the endpoint is actually slow."
						+ COMMON_RULES,
				List.of(apiLatencyTool));
	}

	@Bean
	@Order(2)
	public Specialist connectionAnalysis(ConnectionPoolTool connectionPoolTool) {
		return new Specialist(
				"connection_analysis",
				"Checks whether requests wait for a database connection because the connection pool is exhausted.",
				"Your area is the database connection pool of the service. "
						+ "Determine whether requests are waiting to acquire a connection." + COMMON_RULES,
				List.of(connectionPoolTool));
	}

	@Bean
	@Order(3)
	public Specialist queryAnalysis(SlowQueryTool slowQueryTool) {
		return new Specialist(
				"query_analysis",
				"Finds the SQL statements the service executed and tells which are slow, inefficient, or called too often.",
				"Your area is the SQL statements the service executes. Find the statements behind the reported endpoint "
						+ "and judge whether they are slow, examine far more rows than they return, or run too many times per request."
						+ COMMON_RULES,
				List.of(slowQueryTool));
	}

	@Bean
	@Order(4)
	public Specialist planAnalysis(SlowQueryTool slowQueryTool, QueryPlanTool queryPlanTool) {
		return new Specialist(
				"plan_analysis",
				"Reads the execution plan of a statement and tells how tables are accessed and whether indexes are used.",
				"Your area is the execution plan of the SQL statements behind the reported endpoint. "
						+ "Determine how each table is accessed and whether an index is used. "
						+ "If a previous finding names a statement digest, use it instead of listing the statements again."
						+ COMMON_RULES,
				List.of(slowQueryTool, queryPlanTool));
	}

	@Bean
	@Order(5)
	public Specialist lockAnalysis(LockWaitTool lockWaitTool) {
		return new Specialist(
				"lock_analysis",
				"Checks whether statements are waiting for locks held by other sessions.",
				"Your area is lock contention in the database. "
						+ "Determine whether statements of the service are waiting for locks held by other sessions."
						+ COMMON_RULES,
				List.of(lockWaitTool));
	}

}
