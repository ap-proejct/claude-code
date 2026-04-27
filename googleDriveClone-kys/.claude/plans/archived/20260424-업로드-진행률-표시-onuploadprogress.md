---
task_type: "feat"
title: "업로드 진행률 표시 (onUploadProgress)"
created: "2026-04-24"
archived: "2026-04-24"
score: null
harness_sha: "5810a1b"
absorbed: false
score_auto: 4
eval_notes: "구현·증거 파일 실재, 회고가 한계(중간 진행률 캡처 실패, 드래그 미검증)를 정직하게 기술. 다만 git log 미커밋 상태라 D축 만점은 보류."
eval_at: "2026-04-27"
---
# 업로드 진행률 표시

## Context

현재 파일 업로드 UX 는 "업로드 중..." 텍스트만 노출되고 진행 정도는 알 수 없다. 작은 파일은 괜찮지만 큰 파일·여러 파일 드래그 시 "멈춘 건지, 진행 중인지" 분간이 안 된다. axios `onUploadProgress` 콜백으로 0~100% 집계해 시각화.

두 개의 업로드 경로가 존재:
1. `UploadButton.jsx` — 단일 파일, 버튼 클릭 → 파일 선택 → POST
2. `DrivePage.jsx:55-74` — 드래그&드롭, 여러 파일 순차 업로드

두 경로에 일관된 진행률 표현을 넣는다.

## 구현 스코프

### 공통
- `api.post('/api/files/upload', formData, { headers: ..., onUploadProgress: (e) => ... })` 패턴 적용
- 진행률 계산: `Math.round((e.loaded / e.total) * 100)` (e.total 없을 경우 0 으로 fallback)
- 진행률 상태 변수명: `uploadProgress: number | null` (null = 업로드 아님, 0~100 = 진행 중)

### UploadButton.jsx
- 기존 `uploading: boolean` → `uploadProgress: number | null` 로 대체
- 버튼 텍스트: `null` 이면 "파일 업로드", 숫자면 `업로드 중 {n}%`
- 버튼 내부에 가로 진행바(Tailwind `div` 오버레이, `w-{n}%`) 추가하면 가독성 더 좋음 — 1차 구현에서는 텍스트만, 여유 있으면 바까지

### DrivePage.jsx (드래그 경로)
- `dragUploading: boolean` → `dragUploadState: { current: number, total: number, progress: number } | null`
  - `current` = 지금 업로드 중인 파일 번호 (1-based)
  - `total` = 드롭한 총 파일 수
  - `progress` = 현재 파일의 0~100
- 드래그 오버레이(`isDragging` 영역) 또는 별도 푸터에 `"파일 X/Y 업로드 중 {progress}%"` 표시
- 순차 업로드 루프(`for (const file of ...)`) 에서 매 반복마다 `current` 갱신

### 실패 시 롤백
- try/catch 블록에서 `setUploadProgress(null)` / `setDragUploadState(null)` 확실히 초기화 (기존 `setUploading(false)` 위치 대체)
- 에러 메시지 표시 방식은 현재(`alert`) 유지 — 토스트 도입은 범위 밖 (1-A 와 동일 이유)

## 주요 파일

- `frontend/src/components/UploadButton.jsx` — 단일 업로드 경로 수정
- `frontend/src/pages/DrivePage.jsx` — 드래그 업로드 경로 수정
- `frontend/src/api/axios.js` — 변경 없음 (`onUploadProgress` 는 axios 인스턴스 레벨이 아니라 요청 옵션이므로)

## 검증

- Playwright MCP Flow 2 재실행
  - 작은 파일(10KB) 업로드 후 진행률 바/텍스트가 100% 까지 도달하고 목록에 반영되는지
  - `browser_take_screenshot` 으로 "업로드 중 NN%" 상태 1컷 (빠른 파일이라 캡처 어려우면 큰 파일 써서 시도)
- 드래그 업로드: 파일 2~3개 동시 드롭 후 "1/3 업로드 중 50%" → "2/3 업로드 중 30%" → ... 순서대로 나타나는지 확인
- 실패 케이스: 백엔드 끄고 업로드 시도 → `alert` 뜬 후 상태 null 로 복귀하는지 (버튼 다시 활성화)

## 비범위

- 업로드 취소 버튼 (현재 axios 옵션으로 가능하나 UI 복잡도 증가)
- 재시도·재업로드 플로우
- S3 direct upload (presigned URL) 로 전환 — 현재는 백엔드 경유 multipart 그대로
- 멀티 파일 병렬 업로드 (순차 유지)
- toast 라이브러리 도입
- 진행률 바 애니메이션 세련미 (transition-width 기본값 사용)

## 리스크

- `e.total` 이 `undefined` 인 경우가 있음 (content-length 미세팅). 이때 진행률은 0 으로 계산되어 "업로드 중 0%" 에 머무른 뒤 완료 시점에 UI 가 갑자기 사라지는 경험. 1차 구현은 허용, 관찰되면 "업로드 중 ⋯" indeterminate 로 폴백 추가
- 드래그 경로에서 여러 파일 중간에 실패하면 진행 상태가 애매해짐. 현재 동작은 에러 alert 후 루프 중단. 이미 업로드된 N개 파일은 서버에 남고 프론트 목록만 refetch 안 된 상태. 기존 동작 유지, 해결은 범위 밖
- `multipart/form-data` 경로에서 `onUploadProgress` 이벤트는 요청 바디 전송 진행만 추적. 서버 처리 시간은 잡히지 않아 100% 도달 후 완료 사이에 체감 지연 있을 수 있음

## 후속 메모 (다음 사이클 예고)

- **1-E (신규)**: 파일·폴더를 드래그해서 다른 폴더에 드롭하면 이동. 본 플랜 완료 후 별도 사이클로 진행. 지금 DrivePage 에 이미 "파일 업로드용 드롭 핸들러" 가 있으므로 이벤트 타입 구분(브라우저 파일 vs 내부 아이템) 설계 필요 — 별도 플랜에서 상세화

---

## 회고 (Claude 자동 생성 — 사용자 채점 대기)

### 구현 결과
- **UploadButton.jsx**: `uploading: boolean` → `uploadProgress: 0~100 | null` 로 대체. 버튼 텍스트 `업로드 중 NN%` + 내부 가로 진행바(`bg-blue-800`) 오버레이
- **DrivePage.jsx**: `dragUploading: boolean` → `dragUploadState: { current, total, progress } | null`. 우하단 고정 배너에 `파일 X/Y 업로드 중 NN%` 표시 + 진행바 추가
- 공통: axios 요청 옵션에 `onUploadProgress` 콜백 삽입, `progressEvent.total` 방어 처리(undefined 시 조기 return)
- 실패 시 `finally` 에서 상태 null 복귀 → 버튼 재활성화

### 변경 파일
- `frontend/src/components/UploadButton.jsx`
- `frontend/src/pages/DrivePage.jsx`

### E2E 검증 (Playwright MCP)
- Flow 1 (로그인): ⏭ 세션 유지 상태 사용, 재로그인 안 함
- Flow 2 (업로드): ✅ 부분 통과 — 5MB·100MB 업로드 성공, 201 Created 확인, 버튼 상태 정상 복귀. **단, 진행률 중간(NN%)은 Playwright MCP 의 동기적 특성으로 포착 불가** (file_upload 가 완료까지 블로킹). 코드 경로와 결과로 간접 검증
- Flow 3 (공유): ⏭ 해당 없음
- **스냅샷**: `.claude/logs/e2e/20260424-업로드-진행률/upload-completed-list.png`, `.claude/logs/e2e/20260424-업로드-진행률/upload-network.json`, 요약 로그 `.claude/logs/e2e/20260424-업로드-진행률.md`

### 이슈·학습
- Playwright MCP `browser_file_upload` 는 업로드 완료까지 블로킹 — 중간 상태 스크린샷을 얻으려면 `browser_run_code` 로 비동기 실행 + 타임아웃 스크립트 조합 필요. 하네스 차원에서 "진행 중 UI 캡처" 가 보편적 필요성이 있다면 헬퍼로 뽑을 가치 있음
- 드래그&드롭 업로드 경로는 Playwright 에서 `browser_drag` 로 DOM 엘리먼트 드래그만 지원 — 파일 드롭(브라우저 바깥 → 영역) 은 시뮬레이션 까다로움. 코드 리뷰로만 검증
- 로컬 업로드 속도(100MB/0.7s 쓰기, 네트워크도 사실상 localhost) 로 100MB 도 금방 끝남 → 실제 사용자 체감 시간보다 훨씬 짧아 진행률 UI 의미 체감 어려움. 네트워크 throttle 없이는 적절한 테스트 파일 크기가 애매

### 아쉬움·개선점
- 진행률 중간 상태의 시각적 증거(예: "업로드 중 45%" 스크린샷) 를 남기지 못함. 기능이 동작한다는 간접 증거만. E2E 하네스의 실용적 한계를 드러냄
- 드래그 경로는 코드만 수정하고 E2E 로 검증하지 않음. 단위 테스트(jest + react-testing-library 의 `fireEvent.drop`) 이 자연스러운 보완책이지만 이번 범위 밖
- `onUploadProgress` 가 요청 바디 전송만 측정 → 서버 처리·응답 대기 시간은 반영 안 됨. 100% 후 잠시 "다운로드 마무리" 느낌의 공백 존재. UX 세부 개선은 후속
- 버튼 내부 진행바는 단순 `width: NN%` transition 이라 브라우저 기본 보간 외 애니메이션 디테일 없음

---
**채점 대기**: 위 회고를 읽은 뒤 프론트매터의 `score: null` 을 1~5 정수로 수정하세요.
필요하면 아래 "사용자 채점 메모" 섹션에 본인 의견을 덧붙이세요.

### 사용자 채점 메모
_자유 작성_
