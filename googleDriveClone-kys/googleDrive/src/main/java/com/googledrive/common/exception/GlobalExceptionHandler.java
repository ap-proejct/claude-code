package com.googledrive.common.exception;

import com.googledrive.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.stream.Collectors;

/**
 * 전역 예외 처리 핸들러.
 * 컨트롤러에서 발생하는 예외를 잡아 ApiResponse 형식으로 응답한다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * @Valid 검증 실패 처리.
     * 모든 필드 오류 메시지를 ", "로 합쳐서 반환한다.
     * 예: "이메일은 필수입니다, 비밀번호는 8자 이상이어야 합니다"
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(ApiResponse.error(message));
    }

    /**
     * ResponseStatusException 처리.
     * AuthService에서 던지는 400, 401, 404 에러를 처리한다.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatusException(
            ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode())
                .body(ApiResponse.error(e.getReason()));
    }

    /**
     * 예상치 못한 서버 내부 에러 처리.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("서버 내부 오류가 발생했습니다"));
    }
}
