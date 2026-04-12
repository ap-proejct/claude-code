# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Purpose

**BillAlarmBot** — 구독 서비스 청구서 자동 알림 봇.

Google Sheets에 등록된 구독 항목을 주기적으로 읽어, 마감일 7일 전에:
1. Google Calendar에 일정 자동 추가
2. Telegram Bot으로 알림 발송

사람이 개입 없이 동작하는 완전 자동화 봇이며, 사용자는 결과(Calendar 일정 / Telegram 메시지)만 확인한다.

## Domain Terms

| 용어 | 설명 |
|------|------|
| **구독 항목 (Subscription)** | Google Sheets에서 관리하는 청구 대상 row (서비스명, 금액, 정산일, 결제수단) |
| **정산일 (billingDay)** | 매월 청구 예정 일자(1~31). 이 날짜 기준 -7일에 알림이 트리거됨 |
| **billingDate** | 다음 실제 청구 날짜(LocalDate). billingDay를 기반으로 월말 처리 포함하여 계산 |
| **billingMonth** | billingDate의 월 1일(LocalDate). 중복 알림 방지의 키로 사용됨 |
| **알림 채널** | Google Calendar (일정 생성) + Telegram Bot (메시지 발송) |
| **알림 트리거** | 스케줄러가 매일 실행되어 D-7 항목을 감지하는 이벤트 |
| **복구 스캔** | 앱 재시작 시 D-1~D-7 범위를 전체 스캔하여 누락된 알림을 복구 |

## External Integrations

- **Google Sheets API** — 구독 항목 목록 읽기 (read-only), A:D 열 사용
- **Google Calendar API** — D-7 알림 일정 자동 생성 및 존재 여부 확인
- **Telegram Bot API** — 알림 메시지 발송, 데이터 오류 경고 발송

## Build & Run Commands

```bash
./gradlew build          # Compile and package
./gradlew clean build    # Clean then build
./gradlew bootRun        # Start the application
./gradlew test           # Run all tests
./gradlew test --tests "com.billalarmbot.SomeTest"  # Run a single test class
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

## Stack

- **Java 21**, Spring Boot 4.0.5
- **Spring Web MVC** — HTTP 처리
- **Spring Data JPA** + **MySQL 8.0** — 알림 이력 영속 저장 (중복 방지)
- **H2** — 테스트 환경 전용 인메모리 DB
- **Lombok** — 보일러플레이트 제거 (@Getter, @Builder, @Slf4j, @RequiredArgsConstructor)
- **Gradle 9.4.1** — 빌드 도구
- **Docker Compose** — MySQL 컨테이너 관리

## Environment Variables

앱 실행 전 아래 환경 변수를 설정해야 한다:

| 변수명 | 설명 | 기본값 |
|--------|------|--------|
| `MYSQL_PASSWORD` | MySQL 사용자 비밀번호 | `billalarm1234` |
| `MYSQL_ROOT_PASSWORD` | MySQL root 비밀번호 (Docker용) | `root1234` |
| `TELEGRAM_BOT_TOKEN` | Telegram Bot API 토큰 | (필수) |

Google API 인증은 `src/main/resources/` 에 위치한 서비스 계정 JSON 파일로 처리된다 (`google.credentials.path` 설정 참조).

## Architecture

```
BillAlarmScheduler (Cron + ApplicationReadyEvent)
  ├─ recover()  → D-1~D-7 범위 스캔 (앱 재시작 복구)
  └─ run()      → D-7 정확히 스캔 (매일 정기 실행)
        │
        ├─ SubscriptionService
        │     ├─ GoogleSheetsClient      # Sheets A:D 열 읽기, 행 유효성 검증
        │     ├─ TelegramClient          # 데이터 오류 시 경고 발송
        │     └─ nextBillingDate()       # 월말 처리 포함 다음 정산일 계산
        │
        └─ NotificationService           # 2단계 중복 방지
              ├─ NotificationHistoryRepository  # 1차: DB 이력 확인
              ├─ GoogleCalendarClient    # 2차: Calendar 일정 존재 확인 및 생성
              └─ TelegramClient          # 신규 알림 발송
```

### 2단계 중복 방지 로직 (NotificationService.notify)

1. `NotificationHistory` 테이블에서 `(serviceName, billingMonth)` 조합으로 이력 조회
2. 이력 있으면 → Calendar 일정 존재 여부 확인
   - 일정 없으면 → Calendar 일정만 재생성 (Telegram 재발송 없음)
   - 일정 있으면 → skip
3. 이력 없으면 → Calendar 일정 생성 + Telegram 발송 + DB 이력 저장

### 데이터 유효성 검증 (GoogleSheetsClientImpl)

행 단위로 아래 항목 검증 후 유효 항목만 처리, 오류 행은 Telegram으로 경고:
- A열 (서비스명): 비어있지 않아야 함
- B열 (금액): 숫자여야 함
- C열 (정산일): 1~31 사이의 정수여야 함
- D열 (결제수단): 비어있지 않아야 함

## Source Layout

```
src/main/java/com/billalarmbot/
├─ BillAlarmBotApplication.java
├─ config/
│   ├─ GoogleApiConfig.java          # Google Sheets / Calendar 서비스 빈 등록
│   └─ RestTemplateConfig.java       # RestTemplate 빈 등록
├─ domain/
│   ├─ Subscription.java             # 구독 항목 DTO
│   ├─ NotificationHistory.java      # 알림 이력 JPA 엔티티
│   └─ SheetFetchResult.java         # Sheets 조회 결과 (유효 항목 + 오류 메시지)
├─ client/
│   ├─ GoogleSheetsClient.java       # Interface
│   ├─ GoogleSheetsClientImpl.java
│   ├─ GoogleCalendarClient.java     # Interface
│   ├─ GoogleCalendarClientImpl.java
│   ├─ TelegramClient.java           # Interface
│   └─ TelegramClientImpl.java
├─ repository/
│   └─ NotificationHistoryRepository.java
├─ service/
│   ├─ SubscriptionService.java      # Sheets 읽기, D-7 필터링
│   └─ NotificationService.java      # 알림 발송, 중복 방지
└─ scheduler/
    └─ BillAlarmScheduler.java       # Cron 트리거, 복구 스캔
```

## Subagents

이 프로젝트는 Claude Code Subagent를 아래와 같이 활용한다:

| Agent | 역할 |
|-------|------|
| `domain-agent` | 비즈니스 로직 (D-7 필터링, 중복 방지, 스케줄러) 작성 |
| `integration-agent` | 외부 API 클라이언트 (Sheets / Calendar / Telegram) 연동 코드 작성 |
| `docker-agent` | Docker Compose, MySQL 컨테이너, 데이터베이스 설정 관리 |
| `qa-agent` | 테스트 작성 및 실행 |

Agent 정의 파일: `.claude/agents/`

## Key Configuration (application.yaml)

| 항목 | 키 | 비고 |
|------|----|------|
| MySQL URL | `spring.datasource.url` | localhost:3306/billalarmbot |
| 스케줄 cron | `scheduler.cron` | 기본 매 1분 (`0 */1 * * * *`) |
| Sheets ID | `google.sheets.spreadsheet-id` | |
| Calendar ID | `google.calendar.id` | |
| Telegram chat-id | `telegram.bot.chat-id` | |
| 인증 JSON 경로 | `google.credentials.path` | resources/ 하위 |
