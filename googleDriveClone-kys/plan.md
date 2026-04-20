# Google Drive 클론 코딩 Plan

## 프로젝트 개요
- **목적**: Claude Code를 활용한 효율적인 프로젝트 완성 학습
- **기간**: 최대 1개월
- **방식**: 핵심 로직 위주로 직접 이해, 나머지는 Claude Code 활용

---

## 기술 스택

| 영역 | 기술 |
|---|---|
| Backend | Spring Boot 3.x |
| Frontend | React (Vite) |
| DB | MySQL |
| 파일 저장 | AWS S3 (Free Tier) |
| 인증 | JWT |
| 빌드 | Gradle |

---

## 구현 범위

### Level 1 - 기본 파일 시스템
- 파일 업로드 / 다운로드
- 폴더 생성 / 삭제 / 이름 변경
- 파일 목록 조회 (그리드 / 리스트 뷰)
- 파일 이동 / 복사 (파일 + 폴더 모두)

### Level 2 - 사용자 관리
- 회원가입 / 로그인 (JWT)
- 사용자별 저장 공간 할당 및 사용량 표시
- 파일 소유권

### Level 3 - 공유 기능
- 링크 공유 (읽기 전용 / 편집 가능)
- 사용자 초대 (이메일 기반)
- 권한 관리 (Viewer / Editor)

### Level 4 - 고급 기능
- 파일 미리보기 (이미지, PDF, 텍스트)
- 검색
- 휴지통 / 복원
- 버전 관리

### 사이드바 네비게이션
- 내 드라이브
- 공유 문서함 (다른 사람이 나에게 공유한 파일/폴더)
- 최근 문서함 (내가 최근에 열람한 파일)
- 중요 문서함 (별표 표시한 파일/폴더)
- 휴지통

---

## ERD

### users
| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | BIGINT PK | |
| email | VARCHAR(100) UNIQUE NOT NULL | |
| password | VARCHAR(255) NOT NULL | |
| name | VARCHAR(50) NOT NULL | |
| storage_used | BIGINT DEFAULT 0 | 사용 중인 용량 (bytes) |
| storage_limit | BIGINT DEFAULT 15728640000 | 할당 용량 (기본 15GB) |
| created_at | DATETIME | |
| updated_at | DATETIME | |

### files (파일 + 폴더 통합)
| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | BIGINT PK | |
| name | VARCHAR(255) NOT NULL | |
| item_type | ENUM('FILE','FOLDER') NOT NULL | |
| mime_type | VARCHAR(100) | 폴더면 NULL |
| size | BIGINT | 폴더면 NULL |
| s3_key | VARCHAR(500) | 폴더면 NULL |
| parent_id | BIGINT FK -> files.id | NULL이면 루트 |
| owner_id | BIGINT FK -> users.id | |
| is_starred | BOOLEAN DEFAULT FALSE | |
| is_trashed | BOOLEAN DEFAULT FALSE | |
| trashed_at | DATETIME | 휴지통 들어간 시각 |
| created_at | DATETIME | |
| updated_at | DATETIME | |

### file_shares
| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | BIGINT PK | |
| file_id | BIGINT FK -> files.id | |
| owner_id | BIGINT FK -> users.id | 공유한 사람 |
| shared_with_user_id | BIGINT FK -> users.id | NULL이면 링크 공유 |
| share_token | VARCHAR(100) UNIQUE | NULL이면 유저 공유 |
| permission | ENUM('VIEWER','EDITOR') | |
| expires_at | DATETIME | NULL이면 만료 없음 |
| created_at | DATETIME | |

### file_versions
| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | BIGINT PK | |
| file_id | BIGINT FK -> files.id | |
| version_number | INT NOT NULL | |
| s3_key | VARCHAR(500) NOT NULL | |
| size | BIGINT NOT NULL | |
| created_by | BIGINT FK -> users.id | |
| created_at | DATETIME | |

### file_accesses (최근 문서함용)
| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | BIGINT PK | |
| file_id | BIGINT FK -> files.id | |
| user_id | BIGINT FK -> users.id | |
| accessed_at | DATETIME | |

---

## API 명세

### Auth
| Method | URL | 설명 |
|---|---|---|
| POST | /api/auth/signup | 회원가입 |
| POST | /api/auth/login | 로그인 (JWT 발급) |
| GET | /api/auth/me | 내 정보 조회 |

### 파일 / 폴더
| Method | URL | 설명 |
|---|---|---|
| GET | /api/files | 파일 목록 (?parentId=) |
| POST | /api/files/folder | 폴더 생성 |
| POST | /api/files/upload | 파일 업로드 |
| GET | /api/files/{id} | 파일 상세 조회 |
| PATCH | /api/files/{id}/rename | 이름 변경 |
| PATCH | /api/files/{id}/move | 이동 (파일/폴더) |
| POST | /api/files/{id}/copy | 복사 |
| GET | /api/files/{id}/download | 다운로드 |
| PATCH | /api/files/{id}/star | 별표 토글 |
| DELETE | /api/files/{id} | 휴지통으로 이동 |

### 특수 문서함
| Method | URL | 설명 |
|---|---|---|
| GET | /api/files/recent | 최근 문서함 |
| GET | /api/files/starred | 중요 문서함 |
| GET | /api/files/shared | 공유 문서함 |
| GET | /api/files/trash | 휴지통 |

### 휴지통
| Method | URL | 설명 |
|---|---|---|
| PATCH | /api/files/{id}/restore | 복원 |
| DELETE | /api/files/{id}/permanent | 영구 삭제 |
| DELETE | /api/files/trash | 휴지통 비우기 |

### 공유
| Method | URL | 설명 |
|---|---|---|
| GET | /api/files/{id}/shares | 공유 목록 조회 |
| POST | /api/files/{id}/shares | 공유 추가 (유저/링크) |
| PATCH | /api/files/{id}/shares/{shareId} | 권한 변경 |
| DELETE | /api/files/{id}/shares/{shareId} | 공유 해제 |
| GET | /api/shares/{token} | 링크로 파일 접근 |

### 버전
| Method | URL | 설명 |
|---|---|---|
| GET | /api/files/{id}/versions | 버전 목록 |
| POST | /api/files/{id}/versions/{versionId}/restore | 버전 복원 |

### 검색 / 사용자
| Method | URL | 설명 |
|---|---|---|
| GET | /api/search | 파일 검색 (?q=) |
| GET | /api/users/storage | 저장 공간 사용량 |
