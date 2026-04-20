package com.googledrive.auth;

import com.googledrive.auth.dto.AuthResponse;
import com.googledrive.auth.dto.LoginRequest;
import com.googledrive.auth.dto.ResetPasswordRequest;
import com.googledrive.auth.dto.SecurityQuestionResponse;
import com.googledrive.auth.dto.SignupRequest;
import com.googledrive.config.JwtTokenProvider;
import com.googledrive.user.User;
import com.googledrive.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 인증 관련 비즈니스 로직.
 * - signup: 이메일 중복 검사 → 비밀번호/보안답변 해싱 → User 저장 → JWT 발급
 * - login: 이메일 조회 → 비밀번호 검증 → JWT 발급
 * - getMe: userId로 사용자 조회
 * - getSecurityQuestion: 이메일로 보안 질문 조회 (비밀번호 찾기 1단계)
 * - resetPassword: 이메일 + 답변 검증 후 비밀번호 변경 (2단계)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 회원가입.
     * 이메일 중복 시 400 에러를 반환한다.
     */
    @Transactional
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "이미 사용 중인 이메일입니다");
        }

        // 비밀번호와 보안 답변을 각각 BCrypt로 해싱한다
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        // 보안 답변은 대소문자 무시하고 공백 제거 후 해싱 (사용자 편의를 위해)
        String normalizedAnswer = normalizeAnswer(request.getSecurityAnswer());
        String encodedAnswer = passwordEncoder.encode(normalizedAnswer);

        User user = User.create(
                request.getEmail(),
                encodedPassword,
                request.getName(),
                request.getSecurityQuestion(),
                encodedAnswer
        );
        userRepository.save(user);

        String token = jwtTokenProvider.createToken(user.getId());
        return AuthResponse.of(token, user);
    }

    /**
     * 로그인.
     */
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다");
        }

        String token = jwtTokenProvider.createToken(user.getId());
        return AuthResponse.of(token, user);
    }

    /**
     * 내 정보 조회.
     */
    public User getMe(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"));
    }

    /**
     * 비밀번호 찾기 1단계: 이메일로 보안 질문 조회.
     * 사용자가 존재하지 않거나 보안 질문이 등록되지 않은 경우 404 반환.
     */
    public SecurityQuestionResponse getSecurityQuestion(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "등록되지 않은 이메일입니다"));

        if (user.getSecurityQuestion() == null || user.getSecurityQuestion().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "이 계정은 보안 질문이 등록되어 있지 않습니다");
        }

        return new SecurityQuestionResponse(user.getSecurityQuestion());
    }

    /**
     * 비밀번호 찾기 2단계: 보안 질문 답변 검증 후 비밀번호 변경.
     * 답변 불일치 시 401 반환 (어느 필드가 틀렸는지 구분하지 않음).
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "이메일 또는 답변이 올바르지 않습니다"));

        if (user.getSecurityAnswer() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "이 계정은 보안 질문이 등록되어 있지 않습니다");
        }

        // 사용자가 입력한 답변도 동일하게 정규화 후 검증
        String normalizedAnswer = normalizeAnswer(request.getSecurityAnswer());
        if (!passwordEncoder.matches(normalizedAnswer, user.getSecurityAnswer())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "이메일 또는 답변이 올바르지 않습니다");
        }

        // 새 비밀번호 해싱 후 업데이트
        String encodedNewPassword = passwordEncoder.encode(request.getNewPassword());
        user.changePassword(encodedNewPassword);
    }

    /**
     * 보안 답변 정규화: 대소문자 무시 + 앞뒤 공백 제거.
     * "My Dog" 와 "my dog" 를 같은 답변으로 취급하기 위함이다.
     */
    private String normalizeAnswer(String answer) {
        return answer.trim().toLowerCase();
    }
}
