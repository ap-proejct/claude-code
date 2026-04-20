package com.googledrive.file;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * files 테이블 레포지터리.
 * Spring Data JPA의 메서드 이름 쿼리 자동 생성 기능을 활용한다.
 */
public interface FileItemRepository extends JpaRepository<FileItem, Long> {

    /**
     * 루트 목록 조회 — parentId가 NULL이고 휴지통에 없는 항목.
     * GET /api/files (parentId 파라미터 없을 때)
     */
    List<FileItem> findByOwnerIdAndParentIdIsNullAndIsTrashedFalse(Long ownerId);

    /**
     * 특정 폴더 내 목록 조회 — parentId가 일치하고 휴지통에 없는 항목.
     * GET /api/files?parentId=xxx
     */
    List<FileItem> findByOwnerIdAndParentIdAndIsTrashedFalse(Long ownerId, Long parentId);

    /**
     * 공유된 폴더 하위 조회 — 소유자 무관, parentId가 일치하고 휴지통에 없는 항목.
     * 공유된 폴더를 열람할 때 사용한다 (자식 파일/폴더는 원래 소유자의 것).
     */
    List<FileItem> findByParentIdAndIsTrashedFalse(Long parentId);

    /**
     * 별표 목록 조회 — is_starred=true이고 휴지통에 없는 항목.
     * GET /api/files/starred
     */
    List<FileItem> findByOwnerIdAndIsStarredTrueAndIsTrashedFalse(Long ownerId);

    /**
     * 휴지통 목록 조회 — is_trashed=true인 항목.
     * GET /api/files/trash
     */
    List<FileItem> findByOwnerIdAndIsTrashedTrue(Long ownerId);

    /**
     * 소유자 + ID로 단건 조회 (권한 확인용 단축).
     */
    Optional<FileItem> findByIdAndOwnerId(Long id, Long ownerId);

    /**
     * 파일/폴더 이름 검색 — 대소문자 무시, 휴지통 제외, 최신 수정일 순.
     * GET /api/search?q={keyword}
     */
    @Query("SELECT f FROM FileItem f WHERE f.ownerId = :ownerId " +
           "AND LOWER(f.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "AND f.isTrashed = false " +
           "ORDER BY f.updatedAt DESC")
    List<FileItem> searchByName(@Param("ownerId") Long ownerId, @Param("keyword") String keyword);
}
