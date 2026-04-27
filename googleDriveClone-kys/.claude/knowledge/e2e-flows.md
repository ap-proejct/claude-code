# E2E 골든 패스 (Playwright MCP 수동 검증용)

UI 변경 작업 후 `/task-archive` 이전에 아래 골든 패스를 Playwright MCP 로 수동 실행해 작동을 확인한다. 결과는 회고의 "E2E 검증" 섹션에 기록.

## 전제 조건

- 백엔드 기동: `cd googleDrive && ./gradlew bootRun` (http://localhost:8080)
- 프론트 기동: `cd frontend && npm run dev` — `server.host: true` 필수 (WSL 브라우저 접근용)
- MySQL 컨테이너 up: `docker-compose up -d`
- 테스트 계정: `test@test.com` / `test`
- **WSL↔Windows 토폴로지**: Playwright MCP 는 WSL 에서 chromium 구동. 브라우저의 `localhost` 는 WSL 내부를 가리키므로 프론트·백엔드를 Windows 에서 돌리면 `http://localhost:8080` 하드코딩 호출은 실패함. axios `baseURL` 은 환경변수 `VITE_API_BASE` 또는 `''`(same-origin + vite proxy) 로 풀어둬야 함

MCP 도구(`browser_navigate`, `browser_click`, `browser_type`, `browser_snapshot`) 로 조작. 스냅샷은 각 플로우 종료 시점 1회만 찍는다 (너무 잦으면 컨텍스트 폭주).

## Flow 1: 로그인 → 대시보드 진입

1. `http://localhost:5173/login` 이동
2. 이메일 `test@test.com`, 비밀번호 `test` 입력 후 Submit
3. `/` 또는 `/drive` 로 리다이렉트, 파일 목록 영역 표시되면 통과

**실패 신호**: 401, JWT 저장 실패, AuthContext exp 오판정.

## Flow 2: 파일 업로드 → 목록 반영

1. Flow 1 상태에서 업로드 버튼 클릭 (or 파일 드롭)
2. 작은 텍스트 파일(10KB 미만) 선택
3. 업로드 완료 토스트/진행바 소멸 후 목록에 신규 파일 row 노출되면 통과

**실패 신호**: S3 presigned URL 서명 오류, CORS, 목록 refetch 누락.

## Flow 3: 파일 공유 링크 생성 (읽기 권한)

1. Flow 2 에서 업로드한 파일의 "공유" 메뉴 열기
2. 이메일 `test@test.com` (자기 자신) 또는 별도 계정 입력, 권한 "읽기"
3. 초대 전송 후 "공유됨" 배지/아이콘 반영되면 통과

**실패 신호**: 이메일 유효성 검증, 권한 enum 매핑, 중복 공유 처리.

## 검증 선택 기준

- **UI 수정·Controller/Service 변경**: 관련 Flow 최소 1개 실행
- **순수 백엔드·설정 파일·.claude/ 수정**: E2E 생략 가능 ("해당 없음")
- **인증·권한 경로 수정**: Flow 1 + 영향 Flow 모두 실행

## 회고 기록 포맷

```markdown
### E2E 검증 (Playwright MCP)
- Flow 1 (로그인): ✅ 통과 / ❌ 실패(사유) / ⏭ 해당 없음
- Flow 2 (업로드): ...
- Flow 3 (공유): ...
- **스냅샷**: `<MCP 도구가 저장한 경로 or "미저장">`
```

## 안 하는 것

- 자동 회귀 테스트 (CLI `playwright test`) — 별도 챕터로 보류
- 네거티브 케이스 전수 검증 — 골든 패스만
- 성능·시각 회귀 — 이 하네스 범위 밖

## 실행 로그 위치

실제 실행 결과·이슈 기록은 `.claude/logs/e2e/YYYYMMDD-<요약>.md` 로 분리. 이 문서는 플로우 정의만 유지.

## 증거 캡처 (수동 트리거)

E2E 실행 중 **에러·버그·아쉬운 부분** 을 발견하면 그 즉시 캡처 도구를 호출해 증거를 남긴다. 자동 게이트는 두지 않음 — 판단은 실행 주체가 함.

### 캡처 도구 (상황별)

- **시각 증거가 필요**: `browser_take_screenshot(filename)` — 레이아웃 깨짐, 아이콘/라벨 표기 오류, 모달 잘림 등
- **DOM 구조 증거**: `browser_snapshot(filename)` — 접근성 트리 markdown. 버튼 disabled 고착, 잘못된 role 부여 등 텍스트 diff 가 유의미할 때
- **JS 에러 증거**: `browser_console_messages()` — 결과를 `.txt` 파일로 저장
- **API 증거**: `browser_network_requests()` — 4xx/5xx, presigned URL 서명 오류, 응답 지연 — `.json` 으로 저장

### 저장 경로 컨벤션

로그 md 파일과 **같은 basename 의 디렉토리** 에 번들로 저장.

```
.claude/logs/e2e/
  20260424-flow1-2-3-wsl네이티브-전체통과.md
  20260424-flow1-2-3-wsl네이티브-전체통과/
    flow2-upload-slow-toast.png
    flow2-upload-network.json
    flow3-share-console.txt
```

- 파일명 = `flow<N>-<증상요약>.<확장자>`
- 로그 md 안에서 상대경로로 참조: `![업로드 토스트 지연](20260424-.../flow2-upload-slow-toast.png)`
- 증상 요약은 kebab-case, 한글 가능. 나중에 grep 가능하게 짧고 구체적으로

### 트리거 예시

- 업로드 버튼 클릭 후 목록 갱신 누락 → `browser_take_screenshot` + `browser_network_requests` (업로드 POST 응답 확인용)
- 공유 초대 후 권한 배지 미노출 → `browser_snapshot` (DOM 레벨에서 배지 누락 여부 확정)
- 로그인 리다이렉트 후 콘솔에 React 경고 → `browser_console_messages` 덤프
- 뷰포트 축소 시 레이아웃 깨짐 → `browser_resize` 후 `browser_take_screenshot`
