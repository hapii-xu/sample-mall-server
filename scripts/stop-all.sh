#!/usr/bin/env bash
# Stop the 4 services. Find the process by listening port (8080-8083) and only kill that one.
# (Under Git Bash, $! is an MSYS PID and doesn't match the Windows PID, so we don't rely on pid files.)
cd "$(dirname "$0")/.."
for port in 8080 8081 8082 8083; do
  if command -v netstat >/dev/null 2>&1 && command -v taskkill >/dev/null 2>&1; then
    pid=$(netstat -ano | awk -v p=":$port" '$2 ~ p"$" && $4 == "LISTENING" {print $5; exit}')
    [ -n "$pid" ] && taskkill //F //PID "$pid" >/dev/null 2>&1 && echo "stopped :$port (pid $pid)"
  else
    pid=$(lsof -t -iTCP:"$port" -sTCP:LISTEN 2>/dev/null | head -1)
    [ -n "$pid" ] && kill "$pid" && echo "stopped :$port (pid $pid)"
  fi
done
rm -f logs/*.pid
