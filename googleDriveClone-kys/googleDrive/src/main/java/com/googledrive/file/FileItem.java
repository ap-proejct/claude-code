package com.googledrive.file;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * files 테이블과 매핑되는 엔티티.
 * 파일과 폴더를 통합 관리한다. item_type 컬럼으로 FILE / FOLDER 구분.
 * java.io.File과 이름 충돌을 피하기 위해 FileItem으로 명명.
 */
@Entity
@Table(name = "files")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FileItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 파일 또는 폴더 이름 */
    @Column(nullable = false, length = 255)
    private String name;

    /** FILE 또는 FOLDER */
    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 10)
    private ItemType itemType;

    /** MIME 타입 (폴더는 NULL) */
    @Column(name = "mime_type", length = 100)
    private String mimeType;

    /** 파일 크기 bytes (폴더는 NULL) */
    @Column
    private Long size;

    /** S3 오브젝트 키 (폴더는 NULL, 업로드 전 pending 임시값 사용) */
    @Column(name = "s3_key", length = 500)
    private String s3Key;

    /** 부모 폴더 ID (NULL이면 루트) */
    @Column(name = "parent_id")
    private Long parentId;

    /** 소유자 ID */
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    /** 별표 여부 */
    @Column(name = "is_starred", nullable = false)
    private boolean isStarred = false;

    /** 휴지통 여부 */
    @Column(name = "is_trashed", nullable = false)
    private boolean isTrashed = false;

    /** 휴지통으로 이동된 시각 */
    @Column(name = "trashed_at")
    private LocalDateTime trashedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ──────────────────────────────────────────
    // 정적 팩토리 메서드
    // ──────────────────────────────────────────

    /**
     * 폴더 생성.
     *
     * @param name     폴더 이름
     * @param parentId 부모 폴더 ID (루트이면 null)
     * @param ownerId  소유자 ID
     */
    public static FileItem createFolder(String name, Long parentId, Long ownerId) {
        FileItem item = new FileItem();
        item.name = name;
        item.itemType = ItemType.FOLDER;
        item.parentId = parentId;
        item.ownerId = ownerId;
        item.isStarred = false;
        item.isTrashed = false;
        return item;
    }

    /**
     * 파일 생성.
     *
     * @param name     파일 이름
     * @param mimeType MIME 타입
     * @param size     파일 크기 bytes
     * @param s3Key    S3 오브젝트 키
     * @param parentId 부모 폴더 ID (루트이면 null)
     * @param ownerId  소유자 ID
     */
    public static FileItem createFile(String name, String mimeType, Long size,
                                      String s3Key, Long parentId, Long ownerId) {
        FileItem item = new FileItem();
        item.name = name;
        item.itemType = ItemType.FILE;
        item.mimeType = mimeType;
        item.size = size;
        item.s3Key = s3Key;
        item.parentId = parentId;
        item.ownerId = ownerId;
        item.isStarred = false;
        item.isTrashed = false;
        return item;
    }

    // ──────────────────────────────────────────
    // 상태 변경 메서드 (setter 대신 의미 있는 이름으로)
    // ──────────────────────────────────────────

    /** 이름 변경 */
    public void rename(String newName) {
        this.name = newName;
    }

    /** 부모 폴더 변경 (이동) */
    public void move(Long newParentId) {
        this.parentId = newParentId;
    }

    /** 별표 토글 */
    public void toggleStar() {
        this.isStarred = !this.isStarred;
    }

    /** 휴지통으로 이동 */
    public void trash() {
        this.isTrashed = true;
        this.trashedAt = LocalDateTime.now();
    }

    /** 휴지통에서 복원 */
    public void restore() {
        this.isTrashed = false;
        this.trashedAt = null;
    }

    // ──────────────────────────────────────────
    // 열거형
    // ──────────────────────────────────────────

    public enum ItemType {
        FILE, FOLDER
    }
}
