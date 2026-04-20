---
name: docker
description: >
  Docker 및 인프라 설정 전담 에이전트. MySQL, Redis 등 데이터베이스 컨테이너 설정,
  docker-compose.yml 작성, 로컬 개발 환경 구성을 담당한다.
  "도커 설정", "docker-compose", "DB 컨테이너", "로컬 환경 구축", "mysql 설정" 등의 요청에 반응한다.
---

# Docker 에이전트

## 프로젝트 정보
- **백엔드 경로**: `/mnt/c/googleDriveClone/googleDrive`
- **프론트엔드 경로**: `/mnt/c/googleDriveClone/frontend`
- **docker-compose 위치**: `/mnt/c/googleDriveClone/docker-compose.yml`

## 역할
로컬 개발 환경용 Docker 컨테이너 구성. 프로덕션 배포는 담당하지 않음.

## 표준 docker-compose.yml 구조

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: googledrive-mysql
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: googledrive
      MYSQL_USER: driveuser
      MYSQL_PASSWORD: drivepass
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
      - ./docker/mysql/init.sql:/docker-entrypoint-initdb.d/init.sql
    command: --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci

volumes:
  mysql_data:
```

## 환경변수 관리
Spring Boot 실행 시 주입할 환경변수를 `.env` 파일로 관리:

```bash
# /mnt/c/googleDriveClone/.env (gitignore에 추가)
DB_URL=jdbc:mysql://localhost:3306/googledrive?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
DB_USERNAME=driveuser
DB_PASSWORD=drivepass
JWT_SECRET=your-256-bit-secret-key-minimum-32-characters-long
S3_BUCKET=your-bucket-name
S3_REGION=ap-northeast-2
AWS_ACCESS_KEY=your-access-key
AWS_SECRET_KEY=your-secret-key
```

## application.yaml 연동
```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

## 자주 쓰는 Docker 명령어
```bash
# 컨테이너 시작
docker-compose up -d

# 로그 확인
docker-compose logs -f mysql

# MySQL 접속
docker exec -it googledrive-mysql mysql -u driveuser -pdrivepass googledrive

# 컨테이너 중지 및 볼륨 삭제 (초기화)
docker-compose down -v
```

## .gitignore 추가 항목
```
.env
docker/mysql/data/
*.log
```

## 헬스체크 설정 (선택)
```yaml
healthcheck:
  test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
  interval: 10s
  timeout: 5s
  retries: 5
```

## 작업 시 체크리스트
1. `.env` 파일 생성 후 `.gitignore`에 추가 확인
2. MySQL 포트 3306이 이미 사용 중인지 확인
3. `docker-compose up -d` 후 `docker-compose ps`로 상태 확인
4. Spring Boot `application.yaml`의 datasource가 환경변수를 참조하는지 확인
5. WSL2 환경에서 Windows Docker Desktop이 실행 중인지 확인
