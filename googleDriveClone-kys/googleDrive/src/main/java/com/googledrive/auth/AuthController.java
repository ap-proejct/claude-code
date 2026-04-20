package com.googledrive.auth;

import com.googledrive.auth.dto.AuthResponse;
import com.googledrive.auth.dto.LoginRequest;
import com.googledrive.auth.dto.ResetPasswordRequest;
import com.googledrive.auth.dto.SecurityQuestionResponse;
import com.googledrive.auth.dto.SignupRequest;
import com.googledrive.common.ApiResponse;
import com.googledrive.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 인증 관련 API 컨트롤러.
 *
 * POST /api/auth/signup  - 회원가입
 * POST /api/auth/login   - 로그인 (JWT 발급)
 * GET  /api/auth/me      - 내 정보 조회 (인증 필요)
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 회원가입.
     * 성공 시 201 Created와 함께 JWT 토큰 및 사용자 정보를 반환한다.
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<AuthResponse>> signup(
            @Valid @RequestBody SignupRequest request) {
        AuthResponse response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    /**
     * 로그인.
     * 성공 시 200 OK와 함께 JWT 토큰 및 사용자 정보를 반환한다.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 비밀번호 찾기 1단계: 이메일로 보안 질문을 조회한다.
     * GET /api/auth/security-question?email=...
     * 프론트에서는 이메일을 입력 받아 질문을 받고, 답변을 입력받는 UI로 넘어간다.
     */
    @GetMapping("/security-question")
    public ResponseEntity<ApiResponse<SecurityQuestionResponse>> getSecurityQuestion(
            @RequestParam String email) {
        return ResponseEntity.ok(ApiResponse.success(authService.getSecurityQuestion(email)));
    }

    /**
     * 비밀번호 찾기 2단계: 보안 질문 답변 검증 후 비밀번호를 재설정한다.
     * POST /api/auth/reset-password
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success());
    }

    /**
     * 내 정보 조회.
     * JwtAuthenticationFilter에서 SecurityContext에 저장한 userId를 꺼내 사용한다.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MeResponse>> getMe() {
        Long userId = getCurrentUserId();
        User user = authService.getMe(userId);
        return ResponseEntity.ok(ApiResponse.success(MeResponse.from(user)));
    }

    /** SecurityContext에서 현재 로그인한 사용자의 ID를 추출한다. */
    private Long getCurrentUserId() {
        String name = SecurityContextHolder.getContext().getAuthentication().getName();
        return Long.parseLong(name);
    }

    /**
     * /me 응답용 내부 DTO.
     * 비밀번호는 절대 노출하지 않는다.
     */
    public record MeResponse(
            Long userId,
            String email,
            String name,
            Long storageUsed,
            Long storageLimit
    ) {
        public static MeResponse from(User user) {
            return new MeResponse(
                    user.getId(),
                    user.getEmail(),
                    user.getName(),
                    user.getStorageUsed(),
                    user.getStorageLimit()
            );
        }
    }
}
