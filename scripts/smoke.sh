#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."
base=${BASE_URL:-http://127.0.0.1:8080}
curl -fsS "$base/actuator/health/liveness"
curl -fsS "$base/actuator/health/readiness"
response=$(curl -fsS "$base/api/chat" -H 'Content-Type: application/json' -d '{"model":"assistant","messages":[{"role":"user","content":"hello"}]}')
printf '%s\n' "$response"
printf '%s' "$response" | python3 -c 'import json,sys; r=json.load(sys.stdin); assert r["simulated"] is True; assert r["requestId"]; assert r["usage"]["source"] == "unavailable"; print("mock smoke passed")'
