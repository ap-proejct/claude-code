package com.googledrive.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * users 테이블과 매핑되는 엔티티.
 * 외부에서 new User() 직접 생성을 막고, 정적 팩토리 메서드 create()를 통해서만 생성한다.
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    /** BCrypt 해시된 비밀번호 */
    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    /** 현재 사용 중인 스토리지 용량 (bytes) */
    @Column(name = "storage_used", nullable = false)
    private Long storageUsed = 0L;

    /** 스토리지 한도 - 기본값 15GB (bytes) */
    @Column(name = "storage_limit", nullable = false)
    private Long storageLimit = 15728640000L;

    /** 비밀번호 찾기용 보안 질문 (예: "가장 좋아하는 음식은?") */
    @Column(name = "security_question", length = 255)
    private String securityQuestion;

    /** 보안 질문에 대한 답변의 BCrypt 해시 */
    @Column(name = "security_answer", length = 255)
    private String securityAnswer;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * User 생성을 위한 정적 팩토리 메서드.
     * password와 securityAnswer는 호출 전에 반드시 BCrypt 해싱되어야 한다.
     */
    public static User create(String email, String encodedPassword, String name,
                              String securityQuestion, String encodedSecurityAnswer) {
        User user = new User();
        user.email = email;
        user.password = encodedPassword;
        user.name = name;
        user.storageUsed = 0L;
        user.storageLimit = 15728640000L;
        user.securityQuestion = securityQuestion;
        user.securityAnswer = encodedSecurityAnswer;
        return user;
    }

    /** 비밀번호 변경 (이미 BCrypt 해싱된 값을 받는다) */
    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    /** 스토리지 사용량 증가 */
    public void addStorageUsed(long bytes) {
        this.storageUsed += bytes;
    }

    /** 스토리지 사용량 감소 */
    public void reduceStorageUsed(long bytes) {
        this.storageUsed = Math.max(0, this.storageUsed - bytes);
    }
}
