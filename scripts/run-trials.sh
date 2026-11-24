#!/usr/bin/env bash
# Runs diagnosis trials under one condition and appends one JSON line per trial.
# The target endpoint and the background endpoints keep receiving requests while the diagnosis runs.
#
# usage: [STRATEGY=single-agent] [SKIP_RESTORE=1] scripts/run-trials.sh <condition> <trial count> <output.jsonl>
# conditions: baseline | missing-index | inefficient-sql-condition | lock-wait
set -euo pipefail
cd "$(dirname "$0")/.."

CONDITION=$1
TRIALS=$2
OUTPUT=$3
STRATEGY=${STRATEGY:-single-agent}
START_TRIAL=${START_TRIAL:-1}
MAX_ATTEMPTS=${MAX_ATTEMPTS:-10}
WARMUP_SECONDS=${WARMUP_SECONDS:-30}
LOAD_CONCURRENCY=${LOAD_CONCURRENCY:-2}
ERRORS=${OUTPUT%.jsonl}.errors.jsonl

ORDER_JAR=order-service/build/libs/order-service-0.0.1-SNAPSHOT.jar
ORDER_URL=http://localhost:8080
DIAGNOSIS_URL=http://localhost:8081
LOAD_PIDS=()

mysql_admin() {
	docker exec -i db-fault-diagnosis-mysql mysql -uroot -proot shop "$@" 2>/dev/null
}

index_exists() {
	[ "$(mysql_admin -N -e "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = 'shop' AND table_name = 'orders' AND index_name = 'idx_orders_customer_id'")" != "0" ]
}

restart_order_service() {
	mkdir -p .run
	if [ -f .run/order-service.pid ] && kill -0 "$(cat .run/order-service.pid)" 2>/dev/null; then
		kill "$(cat .run/order-service.pid)"
		while kill -0 "$(cat .run/order-service.pid)" 2>/dev/null; do sleep 0.2; done
	fi
	nohup java -jar "$ORDER_JAR" > .run/order-service.log 2>&1 &
	echo $! > .run/order-service.pid
	until curl -s -m 2 "$ORDER_URL/actuator/health" 2>/dev/null | grep -q UP; do
		kill -0 "$(cat .run/order-service.pid)" 2>/dev/null || { echo "order-service failed to start" >&2; exit 1; }
		sleep 0.5
	done
}

target_request() {
	case "$CONDITION" in
		baseline | missing-index)
			curl -s -o /dev/null -m 120 "$ORDER_URL/api/orders?customerId=$(( (RANDOM * 16) % 500000 + 1 ))" ;;
		inefficient-sql-condition)
			curl -s -o /dev/null -m 120 "$ORDER_URL/api/orders/count?orderedOn=$(date -d "2025-11-14 + $((RANDOM % 365)) days" +%F)" ;;
		lock-wait)
			curl -s -o /dev/null -m 120 -X PATCH "$ORDER_URL/api/orders/$((RANDOM % 1000 + 1))/status" \
				-H "Content-Type: application/json" -d '{"status": "PAID"}' ;;
	esac
}

background_request() {
	local customer=$(( (RANDOM * 16) % 500000 + 1 ))
	case $((RANDOM % 8)) in
		0) curl -s -o /dev/null -m 120 "$ORDER_URL/api/orders/$(( (RANDOM * 150) % 5000000 + 1001 ))" ;;
		1) curl -s -o /dev/null -m 120 "$ORDER_URL/api/orders/recent" ;;
		2) curl -s -o /dev/null -m 120 "$ORDER_URL/api/customers/$customer" ;;
		3) curl -s -o /dev/null -m 120 "$ORDER_URL/api/customers/$customer/order-summary" ;;
		4) curl -s -o /dev/null -m 120 "$ORDER_URL/api/customers/$customer/orders/latest" ;;
		5) curl -s -o /dev/null -m 120 "$ORDER_URL/api/products/$((RANDOM % 10000 + 1))" ;;
		6) curl -s -o /dev/null -m 120 "$ORDER_URL/api/products?category=BOOKS" ;;
		7) curl -s -o /dev/null -m 120 "$ORDER_URL/api/products/category-counts" ;;
	esac
}

start_load() {
	LOAD_PIDS=()
	for _ in $(seq 1 "$LOAD_CONCURRENCY"); do
		( while true; do target_request; sleep 0.1; done ) &
		LOAD_PIDS+=($!)
	done
	( while true; do background_request; sleep 0.1; done ) &
	LOAD_PIDS+=($!)
	if [ "$CONDITION" = "lock-wait" ]; then
		scenarios/lock-wait/hold-locks.sh &
		LOAD_PIDS+=($!)
	fi
}

stop_load() {
	for pid in "${LOAD_PIDS[@]:-}"; do
		[ -n "$pid" ] || continue
		pkill -P "$pid" 2>/dev/null || true
		kill "$pid" 2>/dev/null || true
		{ wait "$pid" || true; } 2>/dev/null
	done
	LOAD_PIDS=()
}

diagnosis_request() {
	case "$CONDITION" in
		baseline | missing-index) echo '{"strategy": "'"$STRATEGY"'", "method": "GET", "path": "/api/orders"}' ;;
		inefficient-sql-condition) echo '{"strategy": "'"$STRATEGY"'", "method": "GET", "path": "/api/orders/count"}' ;;
		lock-wait) echo '{"strategy": "'"$STRATEGY"'", "method": "PATCH", "path": "/api/orders/{id}/status"}' ;;
	esac
}

wait_for_model_api() {
	until [ "$(printf 'header = "x-api-key: %s"\n' "$ANTHROPIC_API_KEY" | curl -s -K - -o /dev/null -m 20 -w "%{http_code}" \
		https://api.anthropic.com/v1/models -H "anthropic-version: 2023-06-01")" = "200" ]; do
		sleep 60
	done
}

set -a
. ./.env
set +a
trap stop_load EXIT

case "$CONDITION" in
	missing-index) index_exists && mysql_admin < scenarios/missing-index/inject.sql ;;
	baseline | inefficient-sql-condition | lock-wait) index_exists || mysql_admin < scenarios/missing-index/restore.sql ;;
	*) echo "unknown condition: $CONDITION" >&2; exit 1 ;;
esac

for trial in $(seq "$START_TRIAL" "$((START_TRIAL + TRIALS - 1))"); do
	for attempt in $(seq 1 "$MAX_ATTEMPTS"); do
		restart_order_service
		mysql_admin -e "TRUNCATE TABLE performance_schema.events_statements_summary_by_digest"
		start_load
		sleep "$WARMUP_SECONDS"

		started_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)
		status=$(curl -s -o .run/response.json -w "%{http_code}" -m 600 -X POST "$DIAGNOSIS_URL/api/diagnoses" \
			-H "Content-Type: application/json" -d "$(diagnosis_request)")
		stop_load
		echo "$STRATEGY $CONDITION trial $trial attempt $attempt: HTTP $status"
		if [ "$status" = "200" ]; then
			jq -c --arg condition "$CONDITION" --argjson trial "$trial" --arg startedAt "$started_at" \
				'{condition: $condition, trial: $trial, startedAt: $startedAt} + .' .run/response.json >> "$OUTPUT"
			break
		fi
		jq -nc --arg strategy "$STRATEGY" --arg condition "$CONDITION" --argjson trial "$trial" --argjson attempt "$attempt" \
			--arg startedAt "$started_at" --arg status "$status" --rawfile body .run/response.json \
			'{strategy: $strategy, condition: $condition, trial: $trial, attempt: $attempt, startedAt: $startedAt, error: {status: $status, body: $body}}' >> "$ERRORS"
		wait_for_model_api
	done
done

if [ "$CONDITION" = "missing-index" ] && [ "${SKIP_RESTORE:-0}" != "1" ]; then
	mysql_admin < scenarios/missing-index/restore.sql
fi
