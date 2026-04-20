package com.googledrive.file;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * file_accesses 테이블 레포지터리.
 */
public interface FileAccessRepository extends JpaRepository<FileAccess, Long> {

    /**
     * 파일 + 사용자 조합으로 접근 기록 조회 (upsert 판단용).
     */
    Optional<FileAccess> findByFileIdAndUserId(Long fileId, Long userId);

    /**
     * 최근 접근 파일 목록 — accessedAt 내림차순, 휴지통 제외.
     * JOIN FETCH로 file을 미리 로딩해 N+1 쿼리를 방지한다.
     * Pageable로 상위 20개만 가져온다.
     *
     * @param userId   현재 사용자 ID
     * @param pageable 페이지 정보 (size=20)
     */
    @Query("SELECT fa FROM FileAccess fa JOIN FETCH fa.file f " +
           "WHERE fa.userId = :userId AND f.isTrashed = false " +
           "ORDER BY fa.accessedAt DESC")
    List<FileAccess> findRecentByUserId(@Param("userId") Long userId, Pageable pageable);
}
