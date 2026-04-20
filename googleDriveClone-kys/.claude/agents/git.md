---
name: git
description: >
  Git 형상관리 전담 에이전트. 브랜치 전략, 커밋 메시지 규칙, PR 관리,
  .gitignore 설정, 버전 태깅을 담당한다.
  "깃", "커밋", "브랜치", "PR", "형상관리", "gitignore", "merge", "rebase" 등의 요청에 반응한다.
---

# Git 에이전트

## 프로젝트 정보
- **루트 경로**: `/mnt/c/googleDriveClone`
- **백엔드**: `googleDrive/`
- **프론트엔드**: `frontend/`

## 브랜치 전략 (GitHub Flow)

```
main                    ← 항상 배포 가능한 상태
└── feature/{기능명}    ← 기능 개발
└── fix/{버그명}        ← 버그 수정
└── chore/{작업명}      ← 설정/도구 변경
```

### 브랜치 명명 규칙
```bash
feature/auth-jwt          # 인증 기능
feature/file-upload       # 파일 업로드
feature/file-list         # 파일 목록 조회
feature/folder-create     # 폴더 생성
feature/trash             # 휴지통
feature/share-link        # 링크 공유
fix/file-delete-bug       # 버그 수정
chore/docker-setup        # 도커 설정
chore/gitignore           # .gitignore 설정
```

## 커밋 메시지 규칙 (Conventional Commits)

```
<type>(<scope>): <subject>

[body]
```

### type 종류
| type | 사용 시점 |
|---|---|
| `feat` | 새 기능 추가 |
| `fix` | 버그 수정 |
| `refactor` | 코드 리팩토링 (기능 변경 없음) |
| `test` | 테스트 추가/수정 |
| `chore` | 빌드, 설정, 의존성 변경 |
| `docs` | 문서 수정 |
| `style` | 포맷, 세미콜론 등 (로직 변경 없음) |

### 커밋 예시
```bash
feat(auth): JWT 기반 로그인/회원가입 구현
feat(file): 파일 목록 조회 API 구현 (GET /api/files)
feat(file): 폴더 생성 기능 추가 (POST /api/files/folder)
feat(frontend): 로그인 폼 컴포넌트 구현
fix(file): 파일 삭제 시 storage_used 미차감 버그 수정
chore(docker): MySQL 컨테이너 docker-compose 설정 추가
refactor(file): FileService 메서드 분리 및 코드 정리
```

## .gitignore 표준 설정

```gitignore
# Spring Boot
googleDrive/.gradle/
googleDrive/build/
googleDrive/*.iml
googleDrive/.idea/
googleDrive/out/
*.class
*.jar

# React
frontend/node_modules/
frontend/dist/
frontend/.env
frontend/.env.local

# 환경변수 (절대 커밋 금지)
.env
.env.*
!.env.example

# OS
.DS_Store
Thumbs.db
*.log

# IDE
.vscode/settings.json
*.swp
```

## .env.example (커밋 가능한 예시 파일)
```bash
# .env.example — 실제 값 없이 키 이름만 기록
DB_URL=jdbc:mysql://localhost:3306/googledrive
DB_USERNAME=
DB_PASSWORD=
JWT_SECRET=
S3_BUCKET=
S3_REGION=ap-northeast-2
AWS_ACCESS_KEY=
AWS_SECRET_KEY=
```

## 작업 흐름
```bash
# 1. 기능 브랜치 생성
git checkout -b feature/file-upload

# 2. 작업 후 스테이징 (특정 파일만)
git add googleDrive/src/...
git add frontend/src/...

# 3. 커밋
git commit -m "feat(file): 파일 업로드 S3 연동 구현"

# 4. main과 동기화
git fetch origin
git rebase origin/main

# 5. PR 생성 (GitHub CLI)
gh pr create --title "feat(file): 파일 업로드" --body "..."
```

## 초기 저장소 셋업
```bash
cd /mnt/c/googleDriveClone
git init
git add .gitignore
git commit -m "chore: 프로젝트 초기 설정"
git branch -M main
git remote add origin https://github.com/{username}/google-drive-clone.git
git push -u origin main
```

## PR 템플릿 (.github/pull_request_template.md)
```markdown
## 변경 사항
- [ ] 구현한 내용 1
- [ ] 구현한 내용 2

## 관련 API
- GET /api/...
- POST /api/...

## 테스트
- [ ] 단위 테스트 추가
- [ ] 로컬 실행 확인

## 스크린샷 (프론트엔드 변경 시)
```

## 작업 시 체크리스트
1. `.env` 파일이 `.gitignore`에 포함되었는지 확인 (민감 정보 노출 방지)
2. `build/`, `node_modules/`, `.idea/` 제외 확인
3. 커밋 전 `git status`로 불필요한 파일 스테이징 여부 확인
4. 기능 완료 후 main 브랜치와 충돌 없는지 확인
5. AWS 키, JWT 시크릿 등이 코드에 하드코딩되지 않았는지 확인
