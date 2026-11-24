#!/usr/bin/env bash
# Runs the full measurement: every strategy under every condition, interleaved by round.
# Trials already recorded in the output file are skipped, so an interrupted run can be resumed.
#
# usage: [STRATEGIES="single-agent ..."] scripts/run-measurement.sh <output.jsonl> [rounds]
set -euo pipefail
cd "$(dirname "$0")/.."

OUTPUT=$1
ROUNDS=${2:-10}
STRATEGIES=${STRATEGIES:-single-agent progressive-collaboration full-collaboration}

recorded() {
	[ -f "$OUTPUT" ] && jq -e -s --arg strategy "$1" --arg condition "$2" --argjson trial "$3" \
		'any(.[]; .strategy == $strategy and .condition == $condition and .trial == $trial)' "$OUTPUT" > /dev/null
}

for condition in baseline inefficient-sql-condition lock-wait missing-index; do
	for round in $(seq 1 "$ROUNDS"); do
		for strategy in $STRATEGIES; do
			if recorded "$strategy" "$condition" "$round"; then
				continue
			fi
			STRATEGY=$strategy START_TRIAL=$round SKIP_RESTORE=1 scripts/run-trials.sh "$condition" 1 "$OUTPUT"
		done
	done
done

docker exec -i db-fault-diagnosis-mysql mysql -uroot -proot shop < scenarios/missing-index/restore.sql 2> /dev/null || true
echo "measurement finished"
