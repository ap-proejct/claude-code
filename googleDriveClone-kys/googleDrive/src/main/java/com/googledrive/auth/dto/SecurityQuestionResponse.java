package com.googledrive.auth.dto;

/**
 * 비밀번호 찾기 1단계 응답: 해당 이메일 사용자의 보안 질문을 반환한다.
 */
public record SecurityQuestionResponse(String securityQuestion) {
}
