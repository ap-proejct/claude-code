package com.googledrive.file.dto;

/**
 * 파일/폴더 이동 요청 DTO.
 * PATCH /api/files/{id}/move
 * parentId가 null이면 루트로 이동한다.
 */
public record MoveRequest(

        /** 이동할 대상 부모 폴더 ID (null이면 루트) */
        Long parentId
) {}
