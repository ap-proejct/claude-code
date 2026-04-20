#!/usr/bin/env bash
# Stop: Claude 한 턴이 끝날 때 최근 5분 안에 바뀐 소스 파일을 최대 10개 보여준다.

CHANGED=$(find /mnt/c/googleDriveClone -maxdepth 6 -type f \
  \( -name '*.java' -o -name '*.jsx' -o -name '*.js' -o -name '*.yaml' -o -name '*.yml' -o -name '*.md' -o -name '*.json' \) \
  -not -path '*/node_modules/*' \
  -not -path '*/build/*' \
  -not -path '*/.gradle/*' \
  -not -path '*/.idea/*' \
  -mmin -5 2>/dev/null | head -10)

if [ -n "$CHANGED" ]; then
  echo '[최근 5분 변경 파일]'
  echo "$CHANGED" | sed 's|/mnt/c/googleDriveClone/|  |'
fi

exit 0
