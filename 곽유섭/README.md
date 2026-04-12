# BillAlarmBot

구독 서비스 청구서 자동 알림 봇.

Google Sheets에 등록된 구독 항목을 매일 자동으로 읽어, **정산일 7일 전**에:
- **Google Calendar**에 알림 일정 자동 추가
- **Telegram**으로 알림 메시지 발송

사람이 개입할 필요 없이 완전 자동으로 동작합니다.

---

## 사전 요구사항

| 항목 | 버전 / 비고 |
|------|------------|
| Java | 21 이상 |
| Docker | Docker Compose 포함 |
| Google Cloud 프로젝트 | Sheets API, Calendar API 활성화 |
| Telegram Bot | BotFather로 생성 |

---

## 1단계 — Google Cloud 설정

### 1-1. 서비스 계정 생성

1. [Google Cloud Console](https://console.cloud.google.com) 접속
2. **APIs & Services → Credentials → Create Credentials → Service Account** 선택
3. 서비스 계정 생성 후 **Keys → Add Key → JSON** 다운로드
4. 다운로드한 JSON 파일을 `src/main/resources/` 아래에 저장

### 1-2. API 활성화

**APIs & Services → Library**에서 아래 두 API를 검색하여 활성화:
- **Google Sheets API**
- **Google Calendar API**

### 1-3. Google Sheets 공유

1. 구독 목록을 관리할 Google Sheets 스프레드시트 생성
2. 스프레드시트의 **공유** 설정에서 서비스 계정 이메일을 **뷰어** 권한으로 초대

> 서비스 계정 이메일은 JSON 파일 내 `client_email` 값입니다.

### 1-4. Google Calendar 공유

1. Google Calendar에서 알림을 받을 캘린더 선택 (또는 새로 생성)
2. **설정 및 공유 → 특정 사용자 또는 그룹과 공유**에서 서비스 계정 이메일을 **일정 변경 권한**으로 초대
3. **캘린더 ID**는 **캘린더 통합** 섹션에서 확인 가능

---

## 2단계 — Google Sheets 데이터 형식

스프레드시트 **1행은 헤더**, **2행부터 데이터**를 입력합니다.

| A열 (서비스명) | B열 (금액) | C열 (정산일) | D열 (결제수단) |
|--------------|-----------|------------|--------------|
| Netflix      | 17000     | 15         | 신용카드        |
| Spotify      | 10900     | 23         | 카카오페이       |

- **서비스명**: 공백 불가
- **금액**: 숫자만 입력 (원 단위)
- **정산일**: 1~31 사이 정수 (월말 자동 처리됨 — 예: 31일 → 2월은 28일)
- **결제수단**: 공백 불가

> 데이터 형식이 잘못된 행은 건너뛰고, Telegram으로 경고 메시지가 발송됩니다.

---

## 3단계 — Telegram Bot 생성

1. Telegram에서 **@BotFather** 검색 후 `/newbot` 명령 실행
2. 봇 이름과 username 입력 → **Bot Token** 발급
3. 봇에게 메시지를 먼저 보낸 후, `https://api.telegram.org/bot{TOKEN}/getUpdates` 에 접속하여 **chat_id** 확인

---

## 4단계 — .env 설정

`.env.example`을 복사하여 `.env` 파일을 만들고, 값을 채워넣습니다:

```bash
cp .env.example .env
```

```env
GOOGLE_CREDENTIALS_PATH=billalarmbot.json   # JSON 파일명
SPREADSHEET_ID=your_spreadsheet_id_here     # Sheets URL의 /d/ 뒤 ID
CALENDAR_ID=your_calendar_id_here           # 캘린더 ID
TELEGRAM_BOT_TOKEN=your_bot_token_here      # BotFather 발급 토큰
TELEGRAM_CHAT_ID=your_chat_id_here          # getUpdates에서 확인한 ID
SCHEDULER_CRON=0 0 9 * * *                  # 알림 시각 (기본: 매일 오전 9시)
MYSQL_PASSWORD=billalarm1234                # 변경 원할 경우
```

> `.env` 파일은 `.gitignore`에 포함되어 있어 git에 커밋되지 않습니다.

---

## 5단계 — 실행

```bash
./setup.sh
```

스크립트가 자동으로:
1. `.env` 필수 항목 및 JSON 파일 존재 여부 확인
2. MySQL Docker 컨테이너 시작 및 준비 대기
3. 앱 실행

---

## 동작 방식

```
앱 시작 시 → D-1~D-7 범위 전체 스캔 (재시작 누락 복구)
매일 cron  → D-7 정확히 해당하는 항목 처리

각 항목 처리 순서:
1. NotificationHistory DB에서 이미 알림을 보냈는지 확인
2. 이력 없으면 → Calendar 일정 생성 + Telegram 발송 + DB 저장
3. 이력 있으면 → Calendar 일정이 삭제됐는지 확인
   - 삭제됐으면 → Calendar 일정만 재생성 (Telegram 재발송 없음)
   - 있으면    → skip
```

### Telegram 알림 예시

**정상 알림:**
```
🔔 정산 D-7 알림

서비스명: Netflix
금액: 17,000원
정산일: 2026-04-22
결제수단: 신용카드
```

**데이터 오류 경고:**
```
⚠️ Sheets 데이터 오류 감지

아래 행의 데이터가 올바르지 않아 처리에서 제외되었습니다:

• 3행: 금액이 숫자가 아닙니다 ('asdf')
• 4행: 정산일이 1~31 사이의 숫자가 아닙니다 ('test')

Google Sheets에서 해당 행을 수정해 주세요.
```

---

## 로그 확인

콘솔 로그 포맷:
```
[2026-04-12 09:00:00] [INFO ] [BillAlarmScheduler] : [BillAlarmScheduler] 정기 스케줄 실행 (D-7)
[2026-04-12 09:00:01] [INFO ] [NotificationService] : Calendar 일정 생성 완료 — Netflix
[2026-04-12 09:00:02] [INFO ] [NotificationService] : Telegram 메시지 발송 완료 — Netflix
```

---

## 프로젝트 구조

```
src/main/java/com/billalarmbot/
├─ config/          # Google API, RestTemplate 설정
├─ domain/          # Subscription, NotificationHistory, SheetFetchResult
├─ client/          # Google Sheets, Calendar, Telegram API 연동
├─ repository/      # NotificationHistory JPA Repository
├─ service/         # 비즈니스 로직 (알림 발송, 중복 방지)
└─ scheduler/       # Cron 스케줄러, 복구 스캔

docker-compose.yml  # MySQL 8.0 컨테이너
```

---

## 개발 / 테스트

```bash
./gradlew test                                               # 전체 테스트
./gradlew test --tests "com.billalarmbot.GoogleSheetsConnectionTest"  # 단일 테스트
```

테스트는 H2 인메모리 DB를 사용하므로 MySQL 컨테이너 없이 실행 가능합니다.
