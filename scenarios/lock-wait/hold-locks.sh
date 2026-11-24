#!/usr/bin/env bash
# Holds row locks on orders 1..1000 in repeated two-second transactions until it is stopped.
set -uo pipefail

while true; do
	docker exec -i db-fault-diagnosis-mysql mysql -ushop_app -pshop_app shop \
		-e "BEGIN; SELECT COUNT(*) FROM (SELECT id FROM orders WHERE id BETWEEN 1 AND 1000 FOR UPDATE) locked; SELECT SLEEP(2); COMMIT;" \
		> /dev/null 2>&1
	sleep 0.3
done
