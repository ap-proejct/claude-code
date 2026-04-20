package com.googledrive.file.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 폴더 생성 요청 DTO.
 * POST /api/files/folder
 */
public record CreateFolderRequest(

        /** 폴더 이름 (필수, 공백 불가) */
        @NotBlank(message = "폴더 이름은 필수입니다")
        @Size(max = 255, message = "폴더 이름은 255자 이하여야 합니다")
        String name,

        /** 부모 폴더 ID (null이면 루트에 생성) */
        Long parentId
) {}
