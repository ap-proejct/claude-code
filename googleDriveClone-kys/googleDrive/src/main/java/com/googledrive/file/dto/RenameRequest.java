package com.googledrive.file.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 파일/폴더 이름 변경 요청 DTO.
 * PATCH /api/files/{id}/rename
 */
public record RenameRequest(

        /** 새 이름 (필수, 공백 불가) */
        @NotBlank(message = "새 이름은 필수입니다")
        @Size(max = 255, message = "이름은 255자 이하여야 합니다")
        String name
) {}
