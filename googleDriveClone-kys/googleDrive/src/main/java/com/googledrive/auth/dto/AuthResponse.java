package com.googledrive.auth.dto;

import com.googledrive.user.User;
import lombok.Getter;

/**
 * 로그인/회원가입 성공 응답 DTO.
 * token: 클라이언트가 이후 요청에서 Authorization 헤더에 담아야 하는 JWT 토큰.
 */
@Getter
public class AuthResponse {

    private final String token;
    private final Long userId;
    private final String email;
    private final String name;

    private AuthResponse(String token, Long userId, String email, String name) {
        this.token = token;
        this.userId = userId;
        this.email = email;
        this.name = name;
    }

    /** User 엔티티와 JWT 토큰으로 응답 객체 생성 */
    public static AuthResponse of(String token, User user) {
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getName());
    }
}
