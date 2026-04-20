---
name: external-api
description: >
  AWS S3, JWT, 이메일 등 외부 API 및 서비스 연동 전담 에이전트.
  파일 업로드/다운로드 S3 연동, presigned URL 생성, JWT 설정, 이메일 공유 초대 기능을 담당한다.
  "S3", "파일 업로드", "presigned URL", "JWT 설정", "이메일 발송", "외부 API" 등의 요청에 반응한다.
---

# External API 에이전트

## 프로젝트 정보
- **백엔드 경로**: `/mnt/c/googleDriveClone/googleDrive`
- **S3 버킷**: 환경변수 `${S3_BUCKET}`, 리전 `${S3_REGION}`
- **JWT**: jjwt 0.12.6, HS256, 24시간 만료

## 역할
S3 파일 저장/조회, JWT 인증 토큰 관리, 외부 서비스 연동.

## AWS S3 설정

### build.gradle 의존성
```groovy
implementation 'software.amazon.awssdk:s3:2.25.11'
```

### S3Config.java
```java
@Configuration
public class S3Config {

    @Value("${aws.credentials.access-key}")
    private String accessKey;

    @Value("${aws.credentials.secret-key}")
    private String secretKey;

    @Value("${aws.s3.region}")
    private String region;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(
                    StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                    )
                )
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(
                    StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                    )
                )
                .build();
    }
}
```

### S3Service.java
```java
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket}")
    private String bucket;

    // 파일 업로드 → S3 키 반환
    public String upload(MultipartFile file, Long userId) {
        String key = "users/" + userId + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .build();

        s3Client.putObject(request, RequestBody.fromInputStream(
            file.getInputStream(), file.getSize()
        ));

        return key;
    }

    // 다운로드용 Presigned URL 생성 (15분 유효)
    public String generatePresignedUrl(String s3Key) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))
                .getObjectRequest(r -> r.bucket(bucket).key(s3Key))
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    // 파일 삭제
    public void delete(String s3Key) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .build());
    }
}
```

## JWT 설정

### application.yaml
```yaml
jwt:
  secret: ${JWT_SECRET}
  expiration: 86400000   # 24시간 (ms)
```

### JwtTokenProvider.java 핵심 메서드
```java
// 토큰 생성
public String createToken(Long userId) {
    return Jwts.builder()
            .subject(userId.toString())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(Keys.hmacShaKeyFor(secretKey.getBytes()))
            .compact();
}

// 토큰에서 userId 추출
public Long getUserId(String token) {
    return Long.parseLong(
        Jwts.parser()
            .verifyWith(Keys.hmacShaKeyFor(secretKey.getBytes()))
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .getSubject()
    );
}
```

## S3 키 네이밍 규칙
```
users/{userId}/{uuid}_{originalFilename}        ← 일반 파일
users/{userId}/versions/{fileId}/{uuid}_{name}  ← 버전 파일
```

## 파일 업로드 플로우
```
1. 클라이언트 → POST /api/files/upload (multipart/form-data)
2. FileController → S3Service.upload() → s3Key 반환
3. File Entity 저장 (s3_key, size, mime_type)
4. User.storage_used 업데이트
5. FileResponse 반환
```

## 로컬 개발 시 S3 대체 (AWS Free Tier 없을 때)
LocalStack 사용:
```yaml
# docker-compose.yml에 추가
localstack:
  image: localstack/localstack
  ports:
    - "4566:4566"
  environment:
    - SERVICES=s3
```

```java
// 로컬용 S3Client (endpoint 오버라이드)
.endpointOverride(URI.create("http://localhost:4566"))
```

## 작업 시 체크리스트
1. `.env`에 AWS_ACCESS_KEY, AWS_SECRET_KEY, S3_BUCKET, S3_REGION 설정
2. S3 버킷 생성 및 퍼블릭 액세스 차단 설정 확인
3. IAM 사용자에 S3 권한 부여 확인
4. 파일 업로드 후 storage_used 업데이트 로직 포함
5. 다운로드는 직접 URL 노출 말고 presigned URL 사용
