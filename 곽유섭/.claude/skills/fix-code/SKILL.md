---
description: BillAlarmBot 코드 수정 skill. domain-agent, integration-agent, qa-agent와 jdtls LSP 플러그인을 활용하여 코드를 수정하고 결과를 검증·보고한다.
---

## 코드 수정 절차

사용자의 수정 요청을 아래 순서대로 처리한다.

---

### 1단계 — 수정 전 LSP 진단 (기준선 확보)

수정할 파일에 대해 LSP로 현재 상태를 확인한다.

- `textDocument/diagnostic` 또는 `textDocument/documentSymbol`로 수정 대상 파일의 현재 오류·경고 목록을 수집한다.
- 수집 결과를 **수정 전 진단 기준선**으로 기록한다.

---

### 2단계 — 수정 담당 에이전트 선택 및 실행

수정 내용의 성격에 따라 아래 기준으로 에이전트를 선택한다. 두 계층을 동시에 건드리는 경우 두 에이전트를 순서대로 실행한다.

| 수정 대상 | 담당 에이전트 |
|-----------|-------------|
| SubscriptionService, NotificationService, BillAlarmScheduler, 도메인 로직 | **domain-agent** |
| GoogleSheetsClient, GoogleCalendarClient, TelegramClient, 외부 API 연동 | **integration-agent** |
| 테스트 파일 (`*Test.java`) | **qa-agent** |
| Docker, MySQL, application.yaml, docker-compose.yml | **docker-agent** |

에이전트 실행 시 수정 요청 내용을 명확하게 전달한다.

---

### 3단계 — 수정 후 LSP 검증

에이전트가 수정을 완료한 직후, 수정된 파일 전체에 대해 LSP 재진단을 실행한다.

- `textDocument/diagnostic`으로 오류(error) / 경고(warning) 목록 수집
- `textDocument/documentSymbol`로 주요 메서드·클래스 구조 확인
- Lombok 애너테이션(@Getter, @Builder, @Slf4j, @RequiredArgsConstructor)은 false positive이므로 오류 판정에서 제외한다

---

### 4단계 — 테스트 실행

수정이 기존 동작을 깨지 않는지 qa-agent에게 테스트를 실행하도록 지시한다.

```bash
./gradlew test
```

테스트 실패 시 qa-agent가 원인을 분석하고 수정 방향을 제시한다.
실패가 수정 범위와 직접 관련이 없으면 그 사실을 명시한다.

---

### 5단계 — 수정 결과 보고

아래 형식으로 최종 보고서를 작성한다.

```
## 코드 수정 결과 보고

### 수정 요약
- 수정 파일: [파일 경로 목록]
- 담당 에이전트: [사용된 에이전트]
- 수정 내용: [변경 사항 요약]

### LSP 진단 결과
- 수정 전: [오류 N건, 경고 N건]
- 수정 후: [오류 N건, 경고 N건]
- 신규 오류: [없음 / 있으면 목록]
- 해소된 오류: [없음 / 있으면 목록]

### 테스트 결과
- 실행: [N건]
- 통과: [N건]
- 실패: [N건 / 없음]
- 실패 원인: [있을 경우 기술]

### 주의사항
[수정 과정에서 발견한 추가 이슈나 권고사항이 있으면 기술]
```

---

## 주의사항

- 요청 범위를 벗어난 파일은 수정하지 않는다.
- 수정 전 LSP 오류가 이미 존재했다면 수정 후에도 동일하게 존재해도 무방하다. 단, 수정으로 인해 **신규 오류가 추가된 경우** 반드시 수정한다.
- `application.yaml`에 하드코딩된 민감 정보(토큰, 비밀번호, API 키)가 포함되려 하면 즉시 중단하고 사용자에게 `.env` 사용을 안내한다.
