---
task_type: "fix"
title: "vite proxy 디버깅 + axios baseURL 환경 분리"
created: "2026-04-23"
archived: "2026-04-24"
score: null
harness_sha: "5810a1b"
absorbed: false
score_auto: 4
eval_notes: "근본 원인 미규명을 솔직히 인정하고 토폴로지 우회로 전환한 사실을 명시, E2E 증거 파일 실재(WSL 네이티브 Flow1·2·3 통과). 다만 D축은 코드 변경이 없어 git diff 정합성 검증 불가, 회고에 적힌 플랜 파일명도 실제와 미세 불일치."
eval_at: "2026-04-27"
---
# vite proxy 디버깅 + axios baseURL 환경 분리

## Context

Playwright MCP Flow 1 최초 실행 중 건진 실전 이슈 2건. 하나는 이미 부분 수정(axios baseURL 환경 분리), 다른 하나는 원인 미규명 상태(vite proxy 룰이 매칭되지 않음).

- axios `baseURL: 'http://localhost:8080'` 하드코딩 → WSL 브라우저에서 `ERR_CONNECTION_REFUSED`. 이 건은 프로덕션 배포 관점에서도 어차피 고쳐야 할 문제였고 E2E 가 선제 발견.
- vite.config.js 에 `proxy: { '/api': 'http://localhost:8080' }` (및 `{ target, changeOrigin, secure }` 객체 형식) 모두 시도. 재시작 후에도 `curl GET /api/x` 가 SPA index.html 반환, `POST /api/*` 가 `Vary: Origin` + Content-Length 0 의 404 (Vite 자체 응답). proxy 가 전혀 매칭되지 않음.

## 범위

### 조사
- Vite 8.0.8 의 proxy 동작 재확인 (기본 동작, plugin-react / @tailwindcss/vite 와의 상호작용)
- `node_modules/.vite` 캐시 초기화 효과 검증
- `npm run dev -- --debug` 로 proxy 매칭 로그 관찰
- 다른 포트/경로 규칙으로 proxy 가 동작하는지 최소 재현 확인
- Windows npm 환경과 WSL 네트워크 경로에서 proxy middleware 가 어디서 막히는지 추적

### 수정 후보
- 객체 형식 + `rewrite` / `ws: true` 등 옵션 조합
- `plugins` 순서 조정 (react / tailwindcss 와 proxy 미들웨어 충돌 가능성)
- Vite 버전 다운그레이드 (8 → 7 또는 7 → 6) 로 회귀 여부 확인
- 최후 수단: frontend 에서 `VITE_API_BASE=http://172.21.80.1:8080` .env 로 주입 + 백엔드 CORS 허용. proxy 없이 직접 호출

### 후속 검증
- `curl http://<host>:5173/api/auth/login` POST → 백엔드 400/401 (Vite 가 아닌 Spring 응답) 수신
- Playwright MCP Flow 1 재실행 → `/` 또는 `/drive` 리다이렉트 및 파일 목록 노출

## 비범위

- Flow 2~3 실행 — Flow 1 통과 후 별도로
- 프로덕션 배포 설정 (리버스 프록시, ingress) — 이번 범위는 dev 환경 한정
- CORS 엔드-투-엔드 설계 — 현재 패턴 유지, proxy 가 붙으면 불필요

## 리스크

- vite 8 가 npm 버전 올라가면서 proxy middleware 가 내부 변경됐을 가능성. 이 경우 업스트림 이슈 확인 + 우회책 결정이 필요
- 프론트를 Windows 에서 굴리고 브라우저를 WSL 에서 굴리는 혼합 토폴로지 자체가 흔치 않아 사례가 부족할 수 있음

---

## 회고 (Claude 자동 생성 — 사용자 채점 대기)

### 구현 결과
- 근본 원인 디버깅은 **미수행**. 대신 2026-04-24 에 스택 전체를 WSL 네이티브로 전환하는 것으로 증상 우회
- WSL 네이티브 환경에서는 동일 vite.config.js 그대로도 proxy 매칭 정상 동작 → 이 이슈는 "WSL 브라우저 ↔ Windows 프론트/백엔드" 혼합 토폴로지 전용 증상으로 결론
- 플랜에 나열했던 조사 항목(`--debug` 로그, 플러그인 순서, vite 다운그레이드 등) 은 **하나도 실행하지 않음**

### 변경 파일
- `.claude/plans/vite-proxy-디버깅-axios-baseurl-환경분리.md` (본 회고 추가)
- 관련 환경 전환 증거는 별도 로그 `.claude/logs/e2e/20260424-flow1-2-3-wsl네이티브-전체통과.md`

### E2E 검증 (Playwright MCP)
- Flow 1 (로그인): ✅ 통과 (WSL 네이티브)
- Flow 2 (업로드): ✅ 통과 (WSL 네이티브)
- Flow 3 (공유): ✅ 통과 (WSL 네이티브)
- **스냅샷**: `.claude/logs/e2e/20260424-flow1-2-3-wsl네이티브-전체통과.md` 참조

### 이슈·학습
- 근본 원인은 여전히 미규명. 혼합 환경으로 다시 돌아갈 일이 생기면 이 플랜을 다시 열어 조사 재개 필요 (**검색 힌트**: "vite proxy 매칭 안 됨", "WSL Windows 혼합 토폴로지")
- "이슈를 스택 전환으로 우회" 가 항상 성립하진 않음. 프로덕션 배포 때 reverse proxy 구성에서 유사 증상이 나올 가능성은 별개

### 아쉬움·개선점
- 조사를 시작하지 않고 토폴로지 전환으로 덮은 점. 단기적 테스트 진행이 우선이었으므로 타협, 다만 지식으로는 공백
- 플랜 생성(2026-04-23) 후 실제 후속 진행 없이 하루 묵힌 점. 플랜 파일이 상태로 남아있으면 다음 세션이 혼란스러워짐 → 이런 경우 "조건부 보류" 라벨을 frontmatter 에 표시할지 검토

---
**채점 대기**: 위 회고를 읽은 뒤 프론트매터의 `score: null` 을 1~5 정수로 수정하세요.
필요하면 아래 "사용자 채점 메모" 섹션에 본인 의견을 덧붙이세요.

### 사용자 채점 메모
_자유 작성_
