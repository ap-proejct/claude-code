package com.googledrive.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * POST /api/auth/reset-password 요청 바디.
 * 이메일 + 보안 질문 답변 + 새 비밀번호를 받아 비밀번호를 재설정한다.
 */
@Getter
@NoArgsConstructor
public class ResetPasswordRequest {

    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email;

    @NotBlank(message = "보안 질문 답변은 필수입니다")
    private String securityAnswer;

    @NotBlank(message = "새 비밀번호는 필수입니다")
    @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다")
    private String newPassword;
}
