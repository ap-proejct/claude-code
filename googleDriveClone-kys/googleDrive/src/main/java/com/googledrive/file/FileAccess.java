package com.googledrive.file;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * file_accesses 테이블과 매핑되는 엔티티.
 * 사용자가 파일/폴더 상세를 조회할 때마다 접근 기록을 upsert하여
 * "최근 문서함" 기능에 활용한다.
 * UNIQUE KEY: (file_id, user_id) — 같은 파일을 여러 번 열어도 레코드 1개만 유지.
 */
@Entity
@Table(
    name = "file_accesses",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_file_user",
        columnNames = {"file_id", "user_id"}
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FileAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 접근한 파일 — 지연 로딩으로 N+1 방지 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private FileItem file;

    /** 접근한 사용자 ID */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 마지막 접근 시각 */
    @Column(name = "accessed_at", nullable = false)
    private LocalDateTime accessedAt;

    /**
     * FileAccess 레코드 생성.
     *
     * @param file   접근한 파일 엔티티
     * @param userId 접근한 사용자 ID
     */
    public static FileAccess of(FileItem file, Long userId) {
        FileAccess access = new FileAccess();
        access.file = file;
        access.userId = userId;
        access.accessedAt = LocalDateTime.now();
        return access;
    }

    /** 접근 시각을 현재 시각으로 갱신 */
    public void updateAccessedAt() {
        this.accessedAt = LocalDateTime.now();
    }
}
