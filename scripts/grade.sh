#!/usr/bin/env bash
# Grades trial records and prints one summary row per strategy and condition.
#
# usage: scripts/grade.sh <records.jsonl>...
set -euo pipefail

jq -s -r '
	def success:
		if .error then false
		elif .condition == "missing-index" then
			.diagnosis.rootCause == "MISSING_INDEX" and .diagnosis.targetTable == "orders" and .diagnosis.targetColumn == "customer_id"
		elif .condition == "inefficient-sql-condition" then
			.diagnosis.rootCause == "INEFFICIENT_SQL_CONDITION" and .diagnosis.targetTable == "orders" and .diagnosis.targetColumn == "ordered_at"
		elif .condition == "lock-wait" then
			.diagnosis.rootCause == "LOCK_WAIT" and .diagnosis.targetTable == "orders"
		else
			.diagnosis.rootCause == "NO_PROBLEM"
		end;
	def mean(f): (map(f) | add) / length;
	group_by([(.strategy // "single-agent"), .condition])[]
	| (map(select(.error | not))) as $ok
	| [
		(.[0].strategy // "single-agent"),
		.[0].condition,
		length,
		(map(select(success)) | length),
		(if ($ok | length) > 0 then ($ok | mean(.inputTokens) | round) else "-" end),
		(if ($ok | length) > 0 then ($ok | mean(.outputTokens) | round) else "-" end),
		(if ($ok | length) > 0 then ($ok | mean(.requestCount) * 100 | round / 100) else "-" end),
		(if ($ok | length) > 0 then ($ok | mean(.durationMs) | round) else "-" end)
	]
	| @tsv
' "$@" | { printf 'strategy\tcondition\ttrials\tsuccess\tavg_input_tokens\tavg_output_tokens\tavg_requests\tavg_duration_ms\n'; cat; } | column -t -s $'\t'
