package com.googledrive.file.dto;

import com.googledrive.file.FileItem;

import java.time.LocalDateTime;

/**
 * 파일/폴더 상세 정보 응답 DTO.
 * Java 16+ record 사용 — 불변 객체이며 자동으로 getter, equals, hashCode, toString을 생성한다.
 */
public record FileResponse(
        Long id,
        String name,
        String itemType,   // "FILE" 또는 "FOLDER"
        String mimeType,
        Long size,
        Long parentId,
        Long ownerId,
        boolean isStarred,
        boolean isTrashed,
        LocalDateTime trashedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    /**
     * FileItem 엔티티를 FileResponse DTO로 변환하는 정적 팩토리 메서드.
     *
     * @param file FileItem 엔티티
     * @return FileResponse DTO
     */
    public static FileResponse from(FileItem file) {
        return new FileResponse(
                file.getId(),
                file.getName(),
                file.getItemType().name(),
                file.getMimeType(),
                file.getSize(),
                file.getParentId(),
                file.getOwnerId(),
                file.isStarred(),
                file.isTrashed(),
                file.getTrashedAt(),
                file.getCreatedAt(),
                file.getUpdatedAt()
        );
    }
}
