#!/usr/bin/env bash
# SessionStart: Claude 세션이 시작될 때 MySQL 컨테이너 상태 안내

STATUS=$(docker ps --filter name=googledrive-mysql --format '{{.Status}}' 2>/dev/null)

if [ -z "$STATUS" ]; then
  echo '[MySQL] 컨테이너 중지 상태 — docker-compose up -d 로 기동 필요'
else
  echo "[MySQL] $STATUS"
fi

exit 0
