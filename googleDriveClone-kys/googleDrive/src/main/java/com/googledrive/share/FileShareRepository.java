package com.googledrive.share;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FileShareRepository extends JpaRepository<FileShare, Long> {

    List<FileShare> findByFileId(Long fileId);

    Optional<FileShare> findByShareToken(String token);

    /**
     * 특정 파일이 특정 사용자에게 공유되어 있는지 확인한다.
     * 만료된 공유는 제외한다.
     * 공유 접근 권한 체크(hasShareAccess)에서 폴더 조상까지 순회하며 사용된다.
     */
    @Query("SELECT COUNT(fs) > 0 FROM FileShare fs " +
           "WHERE fs.fileId = :fileId AND fs.sharedWithUserId = :userId " +
           "AND (fs.expiresAt IS NULL OR fs.expiresAt > CURRENT_TIMESTAMP)")
    boolean existsActiveShareForUser(@Param("fileId") Long fileId, @Param("userId") Long userId);

    @Query("SELECT fs FROM FileShare fs WHERE fs.sharedWithUserId = :userId " +
           "AND (fs.expiresAt IS NULL OR fs.expiresAt > CURRENT_TIMESTAMP)")
    List<FileShare> findActiveSharesWithUser(@Param("userId") Long userId);
}
