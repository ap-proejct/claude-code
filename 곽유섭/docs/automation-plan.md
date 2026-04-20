# BillAlarmBot 자동화 확장 기획

> 작성일: 2026-04-10
> 현재 구현 완료된 기능: Google Sheets 읽기, D-7 필터링, Google Calendar 일정 생성, Telegram 알림 발송

---

## 1. Skills 활용 계획

> **개념**: 자주 쓰는 작업을 `/명령어`로 저장해두고 직접 호출하는 기능.
> 사람이 트리거하는 반복 작업에 적합.

### 적용 아이디어

| 명령어 | 동작 |
|--------|------|
| `/check-sheets` | Sheets 데이터 유효성 검사 (형식 오류, 누락 항목 확인) |
| `/test-notify` | 오늘 날짜 기준으로 D-7 대상 항목 목록 출력 (실제 발송 없이) |
| `/add-subscription` | 새 구독 항목 추가 방법 안내 및 Sheets 링크 제공 |

### 파일 위치
```
.claude/skills/check-sheets.md
.claude/skills/test-notify.md
.claude/skills/add-subscription.md
```

### 구현 시점
- 프로젝트가 어느 정도 안정된 후
- 반복 명령어가 3개 이상 쌓이면 정리

---

## 2. Hooks 활용 계획

> **개념**: Claude Code 내부 이벤트(도구 실행 전/후 등)에 자동으로 반응하는 스크립트.
> 자동화된 가드레일 용도에 적합.

### 적용 아이디어

| 이벤트 | Hook 동작 |
|--------|-----------|
| Java 파일 수정 후 | `./gradlew compileJava` 자동 실행 → 컴파일 오류 즉시 확인 |
| `application.yaml` 수정 시 | 민감 정보(토큰, 키) 하드코딩 여부 경고 |
| `./gradlew bootRun` 실행 후 | 출력 로그에서 ERROR 감지 → Telegram 알림 트리거 |

### 한계
- Spring Boot가 **Claude Code 밖에서** 독립 실행될 경우 로그 감시 불가
- Claude가 직접 `Bash`로 앱을 실행할 때만 `PostToolUse` Hook이 출력을 감지할 수 있음

### 파일 위치
```
.claude/settings.json (hooks 설정)
```

---

## 3. MCP 서버 활용 계획

> **개념**: 외부 서비스를 Claude의 도구로 직접 연결.
> Claude가 코드를 거치지 않고 외부 API를 직접 조작할 수 있음.

### 적용 아이디어

#### Google Sheets MCP
```
"Netflix 정산일 15일로 바꿔줘"
→ Claude가 MCP로 직접 Sheets 수정
```

#### Google Calendar MCP
```
"이번 달 D-7 일정 목록 보여줘"
→ Claude가 MCP로 직접 Calendar 조회
```

#### Telegram MCP (우선순위 높음)
```
Claude가 직접 sendMessage 도구로 Telegram 발송
→ 현재 Java RestTemplate 코드 불필요
→ 유지보수 자동화 시나리오에서 핵심 역할
```

### 구현 우선순위
```
1순위: Telegram MCP    → 유지보수 자동화의 핵심
2순위: Google Sheets MCP → 데이터 직접 조작으로 디버깅 편의성 향상
3순위: Google Calendar MCP → 일정 조회/수정 자동화
```

---

## 4. 유지보수 자동화 기획

> **목표**: Spring Boot 오류 발생 시 자동 감지 → 관리자 Telegram 알림 → 답장으로 수정 지시 → 자동 코드 수정 및 재시작

### 4-1. 전체 흐름

```
Spring Boot 실행 중 오류 발생
         ↓
[Log Watcher] 로그 실시간 감시 (ERROR 패턴 감지)
         ↓
[Claude] 에러 스택트레이스 분석
  - 어떤 오류인지
  - 발생 위치 (파일명, 라인)
  - 예상 원인
  - 수정 방법 제안
         ↓
[Telegram MCP] 관리자에게 알림 발송
  ────────────────────────────────
  🚨 오류 감지

  종류: NullPointerException
  위치: NotificationService.java:47
  원인: billingDate가 null로 전달됨
  제안: null 체크 또는 Optional 처리 추가

  → 수정하려면 "수정해줘" 답장
  → 무시하려면 "무시해" 답장
  ────────────────────────────────
         ↓ 관리자 답장
[Telegram Webhook] 답장 수신 (Spring Boot 엔드포인트)
         ↓
[Claude Code CLI] 코드 자동 수정
  claude --print "47번 라인 수정해줘" --allowedTools Edit,Bash
         ↓
[Bash] 빌드 + 재시작
  ./gradlew build && restart
```

---

### 4-2. 컴포넌트별 기술 스택

| 컴포넌트 | 방법 | 비고 |
|----------|------|------|
| 로그 감시 | Python/bash 스크립트 또는 Spring AOP | `tail -f` 또는 Logback Appender |
| 에러 분석 | Claude (MCP 또는 API 직접 호출) | 스택트레이스 → 분석 결과 |
| Telegram 발송 | **Telegram MCP** | Claude가 직접 도구로 호출 |
| Telegram 수신 | Spring Boot `/webhook` 엔드포인트 | Telegram Bot Webhook 등록 |
| 코드 수정 | Claude Code CLI (`claude --print`) | 자율 실행 모드 |
| 재시작 | Bash 스크립트 | `pkill java && ./gradlew bootRun` |

---

### 4-3. 단계별 구현 계획

#### Step 1 — 에러 감지 + Telegram 알림 (기본)
- Spring Boot AOP로 전역 예외 처리
- 예외 발생 시 기존 `TelegramClient`로 알림 발송
- 오류 종류, 발생 위치만 전달

#### Step 2 — Claude 에러 분석 추가
- Telegram MCP 연결
- 스택트레이스를 Claude에게 전달해서 원인 + 수정 방법 분석
- 분석 결과를 포함한 Telegram 알림 발송

#### Step 3 — 관리자 답장 수신
- Spring Boot에 `/webhook` 엔드포인트 추가
- Telegram Bot Webhook 등록
- 답장 내용에 따라 분기 처리 ("수정해줘" / "무시해" / "로그만 남겨줘")

#### Step 4 — 코드 자동 수정 + 재시작
- Claude Code CLI를 서버에서 실행
- 수정 완료 후 빌드 + 재시작 자동화
- 결과를 Telegram으로 재보고

---

### 4-4. 한계 및 고려 사항

| 항목 | 내용 |
|------|------|
| 보안 | Telegram 답장 발신자 검증 필수 (관리자 Chat ID 화이트리스트) |
| 자동 수정 범위 | 간단한 버그만 가능, 복잡한 로직 변경은 사람이 직접 |
| 무한 루프 위험 | 수정 후에도 오류 반복 시 재시도 횟수 제한 필요 |
| Claude Code CLI | 서버 환경에 Claude Code 설치 및 인증 필요 |
| Webhook 수신 | 로컬 환경에서는 ngrok 등 터널링 도구 필요 |
