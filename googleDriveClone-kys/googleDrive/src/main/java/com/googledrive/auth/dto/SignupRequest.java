package com.googledrive.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * POST /api/auth/signup 요청 바디.
 * @Valid 어노테이션과 함께 사용하면 아래 검증이 자동 실행된다.
 */
@Getter
@NoArgsConstructor
public class SignupRequest {

    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email;

    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다")
    private String password;

    @NotBlank(message = "이름은 필수입니다")
    @Size(max = 50, message = "이름은 50자 이하여야 합니다")
    private String name;

    @NotBlank(message = "보안 질문은 필수입니다")
    @Size(max = 255, message = "보안 질문은 255자 이하여야 합니다")
    private String securityQuestion;

    @NotBlank(message = "보안 질문 답변은 필수입니다")
    @Size(max = 255, message = "답변은 255자 이하여야 합니다")
    private String securityAnswer;
}
