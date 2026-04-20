package com.googledrive.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

/**
 * AWS S3 파일 업로드/다운로드/삭제를 처리하는 서비스.
 */
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket}")
    private String bucket;

    /**
     * MultipartFile을 S3에 업로드하고 s3Key를 반환한다.
     * s3Key 형식: users/{userId}/{uuid}_{originalFilename}
     *
     * @param file   업로드할 파일
     * @param userId 소유자 ID
     * @return 저장된 S3 키
     */
    public String upload(MultipartFile file, Long userId) throws IOException {
        String originalFilename = file.getOriginalFilename() != null
                ? file.getOriginalFilename()
                : "unknown";
        String s3Key = "users/" + userId + "/" + UUID.randomUUID() + "_" + originalFilename;

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .build();

        s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        return s3Key;
    }

    /**
     * 다운로드용 Presigned URL을 생성한다. 유효 기간 15분.
     * Content-Disposition: attachment 헤더를 포함해 브라우저가 바로 다운로드하도록 강제한다.
     *
     * @param s3Key    S3 객체 키
     * @param filename 다운로드 시 저장될 파일 이름
     * @return Presigned URL 문자열
     */
    public String generatePresignedUrl(String s3Key, String filename) {
        // RFC 5987 인코딩: 한글 파일명도 올바르게 처리
        String encoded;
        try {
            encoded = java.net.URLEncoder.encode(filename, "UTF-8").replace("+", "%20");
        } catch (java.io.UnsupportedEncodingException e) {
            encoded = filename;
        }
        String disposition = "attachment; filename*=UTF-8''" + encoded;

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))
                .getObjectRequest(r -> r
                        .bucket(bucket)
                        .key(s3Key)
                        .responseContentDisposition(disposition))
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    /**
     * 이미지 미리보기용 Presigned URL을 생성한다. 유효 기간 15분.
     * Content-Disposition: inline으로 설정해 브라우저에서 바로 표시되도록 한다.
     *
     * @param s3Key    S3 객체 키
     * @param mimeType 파일의 MIME 타입
     * @return Presigned URL 문자열
     */
    public String generateViewUrl(String s3Key, String mimeType) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))
                .getObjectRequest(r -> r
                        .bucket(bucket)
                        .key(s3Key)
                        .responseContentDisposition("inline")
                        .responseContentType(mimeType != null ? mimeType : "application/octet-stream"))
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    /**
     * S3에서 파일을 삭제한다.
     *
     * @param s3Key 삭제할 S3 객체 키
     */
    public void delete(String s3Key) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .build());
    }
}
