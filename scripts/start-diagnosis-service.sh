#!/usr/bin/env bash
# Starts diagnosis-service with ANTHROPIC_API_KEY loaded from .env.
set -euo pipefail
cd "$(dirname "$0")/.."

set -a
. ./.env
set +a
: "${ANTHROPIC_API_KEY:?ANTHROPIC_API_KEY is not set in .env}"

mkdir -p .run
if [ -f .run/diagnosis-service.pid ] && kill -0 "$(cat .run/diagnosis-service.pid)" 2>/dev/null; then
	kill "$(cat .run/diagnosis-service.pid)"
	while kill -0 "$(cat .run/diagnosis-service.pid)" 2>/dev/null; do sleep 0.2; done
fi
nohup java -jar diagnosis-service/build/libs/diagnosis-service-0.0.1-SNAPSHOT.jar > .run/diagnosis-service.log 2>&1 &
echo $! > .run/diagnosis-service.pid
until curl -s -o /dev/null -m 2 http://localhost:8081/api/diagnoses; do
	kill -0 "$(cat .run/diagnosis-service.pid)" 2>/dev/null || { echo "diagnosis-service failed to start" >&2; exit 1; }
	sleep 1
done
echo "diagnosis-service started"
