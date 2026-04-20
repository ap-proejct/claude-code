---
name: api-test
description: >
  Google Drive 클론 API 엔드포인트에 대한 curl 테스트 명령어를 생성한다.
  "API 테스트", "curl 테스트", "엔드포인트 테스트", "테스트 해줘" 등의 요청에 반응한다.
---

# API 테스트 스킬

## 프로젝트 정보
- **Base URL**: `http://localhost:8080`
- **인증**: `Authorization: Bearer {JWT_TOKEN}`
- **Content-Type**: `application/json`

## 사용법

요청받은 API에 대한 curl 명령어를 생성한다.
토큰이 필요한 경우 `TOKEN` 변수를 사용한다.

## 테스트 순서 (처음 실행 시)

```bash
# 1. 회원가입
curl -s -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"password123","name":"테스터"}' | jq

# 2. 로그인 → TOKEN 저장
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"password123"}' | jq -r '.data.token')
echo "TOKEN: $TOKEN"

# 3. 내 정보 확인
curl -s http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer $TOKEN" | jq
```

## API별 curl 템플릿

### 파일 목록
```bash
curl -s "http://localhost:8080/api/files" \
  -H "Authorization: Bearer $TOKEN" | jq

# 특정 폴더
curl -s "http://localhost:8080/api/files?parentId=1" \
  -H "Authorization: Bearer $TOKEN" | jq
```

### 폴더 생성
```bash
curl -s -X POST http://localhost:8080/api/files/folder \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"새 폴더","parentId":null}' | jq
```

### 파일 업로드 (multipart)
```bash
curl -s -X POST http://localhost:8080/api/files/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@/path/to/file.txt" \
  -F "parentId=" | jq
```

### 파일 다운로드 (presigned URL)
```bash
curl -s "http://localhost:8080/api/files/{id}/download" \
  -H "Authorization: Bearer $TOKEN" | jq
```

### 이름 변경
```bash
curl -s -X PATCH http://localhost:8080/api/files/{id}/rename \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"새이름.txt"}' | jq
```

### 별표 토글
```bash
curl -s -X PATCH http://localhost:8080/api/files/{id}/star \
  -H "Authorization: Bearer $TOKEN" | jq
```

### 휴지통으로 이동
```bash
curl -s -X DELETE http://localhost:8080/api/files/{id} \
  -H "Authorization: Bearer $TOKEN" | jq
```

### 공유 링크 생성
```bash
curl -s -X POST http://localhost:8080/api/files/{id}/shares \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"type":"LINK","permission":"VIEWER"}' | jq
```

## 응답 형식 확인 포인트
- `success: true` 여부
- `data` 필드 내용
- HTTP 상태 코드 (401 인증 실패, 403 권한 없음, 404 파일 없음)
