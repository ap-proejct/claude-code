# Claude Calendar Automation

Discord 메시지로 Google Calendar를 자동 관리하는 토이 프로젝트.
**Python 봇 없이** Claude Code + Claude Channels + gws CLI만으로 동작합니다.

## 아키텍처

```
Discord 메시지
    ↓
Claude Channels (Discord MCP)
    ↓
Claude Code — 자연어 이해 + gws 실행
    ↓
Google Calendar API
    ↓
Claude Channels → Discord 답장
```

Claude Code가 Discord 봇 역할을 직접 수행합니다.
별도 서버나 Python 스크립트가 필요 없습니다.

## 사전 요구사항

```bash
# Google Workspace CLI 설치
npm install -g @googleapis/gws

# Google 계정 인증
gws auth login
```

## Discord 설정

| 항목 | 위치 |
|------|------|
| 봇 토큰 | `~/.claude/channels/discord/.env` |
| 접근 권한 | `~/.claude/channels/discord/access.json` |
| 채널 등록 | `/discord:access` 스킬 |

현재 채널 구성:
- `1219889462612594771` — 멘션 필요 (`@봇 내일 오후 3시 미팅 추가해줘`)
- `1487680921334321194` — 멘션 없이 메시지만으로 트리거

## 사용 예시

```
[Discord]
사용자: @캘린더봇 이번 주 금요일 오후 2시 치과 예약 잡아줘
봇:     ✅ 4월 11일(금) 오후 2:00 "치과 예약" 등록했습니다.

사용자: 이번 주 일정 알려줘
봇:     📅 이번 주 일정
        - 4월 8일(화) 10:00 스탠드업
        - 4월 11일(금) 14:00 치과 예약
```

## 지원 기능

| 기능 | 예시 |
|------|------|
| 일정 등록 | "내일 오후 3시 팀 미팅 추가해줘" |
| 일정 조회 | "이번 주 일정 보여줘" |
| 일정 수정 | "내일 미팅 4시로 바꿔줘" |
| 일정 삭제 | "금요일 약속 취소해줘" |
| 빈 시간 조회 | "다음 주 비어있는 시간 알려줘" |

## 로드맵

- [ ] 반복 일정 지원
- [ ] 여러 캘린더 관리
- [ ] Slack 채널 연동
