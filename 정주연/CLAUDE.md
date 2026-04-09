# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

Claude Channels(Discord MCP)를 통해 Discord 메시지를 수신하고, `gws` CLI로 Google Calendar를 CRUD 조작하는 자동화 프로젝트.
Python 봇 없이 Claude Code 자체가 Discord 봇 역할을 합니다.

```
Discord 메시지 → Claude Channels(MCP) → Claude Code → gws CLI → Google Calendar
                                                      ↓
                                               Discord 답장
```

## Discord 메시지 처리 흐름

Discord 메시지가 들어오면 아래 순서로 처리한다:

1. 메시지에서 **의도** 파악 (등록 / 수정 / 삭제 / 조회 / 주간요약 / 빈시간)
2. **날짜·시간·제목** 추출 — 모호하면 먼저 질문
3. 수정·삭제는 아래 **수정·삭제 플로우** 참고
4. `gws calendar` 명령 실행
5. 결과를 한국어로 Discord에 답장

## 수정·삭제 플로우

"오늘 3시 미팅 취소해줘", "내일 회의 시간 바꿔줘" 같은 요청:

1. 언급된 날짜 기준으로 `events list` 조회
2. 후보 목록을 번호 붙여 표시:
   ```
   다음 일정 중 어떤 것을 [수정/삭제]할까요?
   1. 팀 회의 (14:00~15:00)
   2. 점심 약속 (12:00~13:00)
   ```
3. 사용자가 번호로 답변 → 해당 eventId로 실행
4. **수정** 시: 변경할 내용(제목/시간)이 메시지에 없으면 추가로 질문
5. **삭제** 시: 번호 확인 후 즉시 삭제 (재확인 불필요)
6. 후보가 1개뿐이어도 반드시 확인 후 실행

## 주간 요약

"이번 주 일정", "주간 일정", "이번 주 뭐 있어" 등의 요청:

- `timeMin`: 이번 주 월요일 00:00:00+09:00
- `timeMax`: 이번 주 일요일 23:59:59+09:00
- 출력 형식: 날짜별로 그룹화, 이벤트 없는 날은 생략
  ```
  [월] 4/7
  - 팀 회의 14:00~15:00

  [수] 4/9
  - 점심 약속 12:00~13:00
  - 발표 준비 15:00~17:00
  ```
- 이번 주 일정이 없으면 "이번 주 등록된 일정이 없습니다." 로 답장

## gws 핵심 커맨드

```bash
# 스키마 확인 (호출 전 필요시 참고)
gws schema calendar.events.insert

# 이벤트 생성 (calendarId는 --params 또는 --json 안에)
gws calendar events insert \
  --params '{"calendarId":"primary"}' \
  --json '{"summary":"제목","start":{"dateTime":"2026-04-07T14:00:00+09:00"},"end":{"dateTime":"2026-04-07T15:00:00+09:00"}}'

# 이벤트 조회
gws calendar events list \
  --params '{"calendarId":"primary","timeMin":"2026-04-07T00:00:00+09:00","timeMax":"2026-04-14T00:00:00+09:00","singleEvents":true,"orderBy":"startTime"}'

# 이벤트 수정
gws calendar events patch \
  --params '{"calendarId":"primary","eventId":"{id}"}' \
  --json '{"summary":"새 제목"}'

# 이벤트 삭제
gws calendar events delete \
  --params '{"calendarId":"primary","eventId":"{id}"}'

# 빈 시간 조회
gws calendar freebusy query \
  --json '{"timeMin":"...","timeMax":"...","items":[{"id":"primary"}]}'
```

## 자연어 → API 변환 규칙

| 입력 | 변환 |
|------|------|
| "내일" | 현재 날짜 +1일 (Asia/Seoul, UTC+9) |
| "오후 3시" | `15:00:00+09:00` |
| 시간 미지정 | 종일 이벤트 — `"date": "YYYY-MM-DD"` |
| 소요시간 미지정 | 기본 1시간 |
| "이번 주" | 당일 기준 가장 가까운 월요일~일요일 |
| "다음 주" | 다음 월요일~일요일 |
| "오늘 + 내일" | 당일 00:00 ~ 익일 23:59 (데일리 브리핑용) |

## Discord 채널 설정

프로젝트 루트 `.env`에서 관리합니다. 세션 시작 시 훅이 자동으로 `~/.claude/channels/discord/`에 적용합니다.

```bash
# .env
DISCORD_BOT_TOKEN=봇_토큰
ALLOWED_CHANNEL_IDS=채널_ID          # 쉼표로 여러 채널 지정
ALLOWED_USER_IDS=사용자_ID           # 쉼표로 여러 사용자 지정
REQUIRE_MENTION=true                 # false면 멘션 없이도 응답
```

적용 방식: `UserPromptSubmit` 훅 → `.claude/hooks/apply-discord-config.sh` 실행
`.env`가 없으면 훅은 아무것도 하지 않습니다.

## 작업 수칙

- **삭제·수정 전** 반드시 조회 후 확인
- **날짜·시간 모호** 시 실행 전에 질문
- 답장은 한국어로 간결하게 (이모지 불필요)
