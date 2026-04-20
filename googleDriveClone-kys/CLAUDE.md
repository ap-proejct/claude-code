# Google Drive Clone

Spring Boot 4.0.5 + React 19로 만드는 Google Drive 클론. **학습 목적 프로젝트**이며, 사용자는 Spring Boot 경험 있는 React 초보자이다.

---

## 기술 스택

- **Backend**: Spring Boot 4.0.5, Java 21, Lombok, JPA/Hibernate 7, Spring Security, JJWT 0.12.6, AWS S3 SDK 2.25
- **Frontend**: React 19, Vite 8, TailwindCSS 4, Axios, React Router 7
- **DB**: MySQL 8.0 (Docker, utf8mb4 강제 설정)
- **빌드**: Gradle (백엔드) / npm (프론트엔드)

## 주요 경로

| 용도 | 경로 |
|------|------|
| 백엔드 루트 | `/mnt/c/googleDriveClone/googleDrive` |
| 프론트엔드 루트 | `/mnt/c/googleDriveClone/frontend` |
| 환경변수 | `/mnt/c/googleDriveClone/.env` |
| Docker 설정 | `/mnt/c/googleDriveClone/docker-compose.yml` |

## 실행 명령어

```bash
# MySQL (Docker)
docker-compose up -d

# 백엔드 (WSL 터미널 기준)
cd googleDrive
while IFS='=' read -r key value; do [[ "$key" =~ ^[[:space:]]*# ]] && continue; [[ -z "$key" ]] && continue; export "$key=$value"; done < ../.env
./gradlew bootRun

# 프론트엔드 (Windows npm 권장 - WSL과 node_modules 혼용 금지)
cd frontend
npm run dev
```

IntelliJ에서 백엔드 실행 시 **Run Configuration의 Environment variables**에 `.env` 내용을 복사해 넣거나 **EnvFile 플러그인** 사용.

---

## 핵심 컨벤션

### Frontend
- 사용자는 React 초보자 → 컴포넌트/함수마다 **한국어 주석 필수**
- Tailwind 유틸리티 클래스 우선 사용
- API 호출은 `src/api/axios.js`의 `api` 인스턴스로 (JWT 자동 첨부)
- 상태는 컴포넌트 로컬 `useState` 우선, 공용은 `AuthContext`

### Backend
- DTO는 Java `record` 타입 사용
- Service는 `@Transactional(readOnly = true)` 기본, 쓰기 작업에 별도 `@Transactional`
- Controller 응답은 `ApiResponse<T>` 래퍼로 감싸기
- 예외는 `ResponseStatusException` + 적절한 HTTP Status
- `@RequiredArgsConstructor`로 생성자 주입

### 파일 접근 권한 (FileService)
- 소유자 검증: `findAndVerifyOwner()` — 쓰기 작업용
- 접근 권한 검증: `findAndVerifyAccess()` — 소유자 또는 공유 받은 사용자 허용 (읽기/다운로드/미리보기)
- 공유 폴더의 하위 파일은 owner 무관하게 접근 허용 (`hasShareAccess` 조상 순회)

---

## 테스트 계정

| 이메일 | 비밀번호 | 보안 질문 답변 |
|--------|---------|------------|
| test@test.com | test | test (질문: 가장 좋아하는 음식은?) |
| rhkdbtj@test.com | (재설정 필요 시 `/forgot-password`) | test (질문 동일) |

---

## 환경 주의사항

- **WSL2 + Windows 혼용**: 프론트엔드 `node_modules`는 Windows Node에서 설치 (Linux 바이너리와 혼용 시 `vite not found` 발생)
- **Vite HMR**: `vite.config.js`에 `usePolling: true` 설정됨 (Windows filesystem 호환)
- **MySQL 한글 처리**: `docker-compose.yml`에 `--skip-character-set-client-handshake`와 `--init-connect=SET NAMES utf8mb4` 설정돼 있어 `docker exec mysql -e` 에서 별도 charset 옵션 불필요
- **JWT 만료**: 24시간 (86400000ms). 프론트엔드 `AuthContext`가 앱 로드 시 `exp` 클레임을 디코드해 만료 자동 확인

---

## 작업 방침

- **멀티스텝 작업**: `TaskCreate`로 할일 관리, 완료 즉시 `TaskUpdate` (배치 말고 즉시)
- **구조적 변경**: Plan 모드(`Shift+Tab` 2번) 진입 후 사용자 승인 받기
- **코드 작성보다 탐색이 더 많을 때**: `Explore` 서브에이전트로 위임해 메인 컨텍스트 보호
- **도메인별 서브에이전트**:
  - `backend` — Spring Boot API/Service/Entity
  - `frontend` — React 컴포넌트, 상태관리
  - `docker` — docker-compose, DB 컨테이너
  - `erd` — DB 설계, JPA 엔티티, 인덱스
  - `external-api` — S3, JWT, 이메일 연동
  - `security-reviewer` — 인증/권한 코드 보안 검토

## 금지 사항

- `.env` 파일 직접 편집 금지 (hook으로 차단됨)
- 사용자 확인 없이 destructive git 명령 실행 금지 (`reset --hard`, `push --force`)
- `node_modules`를 WSL/Windows 간 공유 금지
- 커밋은 사용자가 명시적으로 요청할 때만
