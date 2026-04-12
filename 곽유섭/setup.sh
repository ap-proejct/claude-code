#!/bin/bash
set -e

echo "=============================="
echo "  BillAlarmBot Setup"
echo "=============================="

# .env 파일 확인
if [ ! -f ".env" ]; then
  echo ""
  echo "[오류] .env 파일이 없습니다."
  echo "  cp .env.example .env 후 값을 채워주세요."
  exit 1
fi

# .env 로드
export $(grep -v '^#' .env | grep -v '^$' | xargs)

# 필수 항목 확인
MISSING=()
[ -z "$GOOGLE_CREDENTIALS_PATH" ] && MISSING+=("GOOGLE_CREDENTIALS_PATH")
[ -z "$SPREADSHEET_ID" ]          && MISSING+=("SPREADSHEET_ID")
[ -z "$CALENDAR_ID" ]             && MISSING+=("CALENDAR_ID")
[ -z "$TELEGRAM_BOT_TOKEN" ]      && MISSING+=("TELEGRAM_BOT_TOKEN")
[ -z "$TELEGRAM_CHAT_ID" ]        && MISSING+=("TELEGRAM_CHAT_ID")

if [ ${#MISSING[@]} -gt 0 ]; then
  echo ""
  echo "[오류] .env에서 아래 항목이 비어 있습니다:"
  for item in "${MISSING[@]}"; do
    echo "  - $item"
  done
  exit 1
fi

# 서비스 계정 JSON 확인
JSON_PATH="src/main/resources/$GOOGLE_CREDENTIALS_PATH"
if [ ! -f "$JSON_PATH" ]; then
  echo ""
  echo "[오류] Google 서비스 계정 JSON 파일을 찾을 수 없습니다: $JSON_PATH"
  echo "  파일을 src/main/resources/ 아래에 저장하고 .env의 GOOGLE_CREDENTIALS_PATH를 확인해주세요."
  exit 1
fi

echo ""
echo "[1/3] .env 설정 확인 완료"

# MySQL 컨테이너 시작
echo "[2/3] MySQL 컨테이너 시작 중..."
docker compose up -d

echo "      MySQL 준비 대기 중..."
until docker compose exec -T mysql mysqladmin ping -h localhost --silent 2>/dev/null; do
  sleep 2
done
echo "      MySQL 준비 완료"

# 앱 실행
echo "[3/3] BillAlarmBot 시작 중..."
echo ""

if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "win32" ]]; then
  ./gradlew.bat bootRun
else
  ./gradlew bootRun
fi
