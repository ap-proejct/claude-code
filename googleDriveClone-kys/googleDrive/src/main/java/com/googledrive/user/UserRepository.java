package com.googledrive.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * User 엔티티의 DB 접근 인터페이스.
 * JpaRepository를 상속받아 기본 CRUD 메서드가 자동 제공된다.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /** 이메일로 사용자 조회 (로그인, 중복 체크에 사용) */
    Optional<User> findByEmail(String email);

    /** 이메일 중복 여부 확인 */
    boolean existsByEmail(String email);
}
