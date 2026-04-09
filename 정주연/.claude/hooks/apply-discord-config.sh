#!/bin/bash
# .env → .claude/discord/ 동기화
# UserPromptSubmit 훅으로 실행됩니다.

PROJECT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
ENV_FILE="$PROJECT_DIR/.env"
STATE_DIR="$PROJECT_DIR/.claude/discord"

[ ! -f "$ENV_FILE" ] && exit 0

# .env 파싱
while IFS='=' read -r key value; do
  [[ "$key" =~ ^#.*$|^$ ]] && continue
  export "$key=$value"
done < "$ENV_FILE"

[ -z "$DISCORD_BOT_TOKEN" ] && exit 0

GLOBAL_DIR="$HOME/.claude/channels/discord"
mkdir -p "$STATE_DIR" "$GLOBAL_DIR"

# 봇 토큰 저장 (프로젝트 로컬 + 전역)
echo "DISCORD_BOT_TOKEN=$DISCORD_BOT_TOKEN" > "$STATE_DIR/.env"
echo "DISCORD_BOT_TOKEN=$DISCORD_BOT_TOKEN" > "$GLOBAL_DIR/.env"
chmod 600 "$STATE_DIR/.env" "$GLOBAL_DIR/.env"

# 허용 사용자 → JSON 배열
USER_ARRAY="[]"
if [ -n "$ALLOWED_USER_IDS" ]; then
  USER_ARRAY="[$(echo "$ALLOWED_USER_IDS" | sed 's/,/","/g; s/^/"/; s/$/"/'  )]"
fi

# 채널 groups 생성
REQUIRE="${REQUIRE_MENTION:-true}"
GROUPS_JSON="{}"
if [ -n "$ALLOWED_CHANNEL_IDS" ]; then
  GROUPS_JSON="{"
  IFS=',' read -ra CHANNELS <<< "$ALLOWED_CHANNEL_IDS"
  FIRST=true
  for CH in "${CHANNELS[@]}"; do
    CH=$(echo "$CH" | tr -d ' ')
    [ "$FIRST" = true ] && FIRST=false || GROUPS_JSON="$GROUPS_JSON,"
    GROUPS_JSON="${GROUPS_JSON}\"$CH\":{\"requireMention\":$REQUIRE,\"allowFrom\":$USER_ARRAY}"
  done
  GROUPS_JSON="$GROUPS_JSON}"
fi

# access.json 저장 (프로젝트 로컬 + 전역)
ACCESS_JSON=$(cat << EOF
{
  "dmPolicy": "allowlist",
  "allowFrom": $USER_ARRAY,
  "groups": $GROUPS_JSON,
  "pending": {}
}
EOF
)
echo "$ACCESS_JSON" > "$STATE_DIR/access.json"
echo "$ACCESS_JSON" > "$GLOBAL_DIR/access.json"

exit 0
