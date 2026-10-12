#!/usr/bin/env bash
# Build if needed, start all 4 services in the background, and wait for health checks.
# Logs go to logs/<svc>.log; stop with scripts/stop-all.sh (finds processes by port).
set -euo pipefail
cd "$(dirname "$0")/.."

if [ ! -f mall-gateway/target/mall-gateway-0.1.0.jar ] || [ "${REBUILD:-0}" = "1" ]; then
  ./mvnw -q -B -DskipTests package
fi

mkdir -p logs
for svc in user product order gateway; do
  nohup java -jar "mall-$svc/target/mall-$svc-0.1.0.jar" > "logs/$svc.log" 2>&1 &
done

echo "waiting for services..."
for _ in $(seq 1 90); do
  up=0
  for port in 8080 8081 8082 8083; do
    curl -s --noproxy '*' -m 2 "http://localhost:$port/actuator/health" | grep -q '"UP"' && up=$((up + 1))
  done
  if [ "$up" = 4 ]; then
    echo "all services UP: gateway http://localhost:8080"
    exit 0
  fi
  sleep 2
done
echo "services did not come up in time; check logs/*.log" >&2
exit 1
