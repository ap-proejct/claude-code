# user 도메인

## 책임

사용자 인증(회원가입·로그인·로그아웃), 사용자 프로필, 스토리지 사용량 관리.

## 파일 구성

```
user/
├── controller/
│   ├── AuthController.kt       GET/POST /auth/login, /auth/register, /auth/logout
│   ├── UserController.kt       GET/POST /profile/**
│   └── dto/
│       ├── RegisterRequest.kt
│       ├── LoginRequest.kt
│       └── UserResponse.kt
├── service/
│   └── UserService.kt
├── domain/
│   ├── User.kt                 도메인 엔티티 + JPA 어노테이션
│   └── UserRepository.kt       인터페이스
└── infrastructure/
    └── UserJpaRepository.kt    Spring Data JPA 구현체
```

## User 엔티티 핵심 필드

```kotlin
@Entity @Table(name = "users")
class User(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(unique = true, nullable = false)
    var email: String,

    @Column(nullable = false)
    var password: String,          // BCrypt 해시

    @Column(nullable = false)
    var name: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "system_role", nullable = false)
    var systemRole: SystemRole = SystemRole.USER,

    @Column(name = "storage_limit_bytes", nullable = false)
    var storageLimitBytes: Long = 16_106_127_360L,   // 15GB

    @Column(name = "storage_used_bytes", nullable = false)
    var storageUsedBytes: Long = 0L,
)

enum class SystemRole { USER, ADMIN }
```

## UserService 주요 메서드

```kotlin
fun register(email: String, password: String, name: String): User
fun findByEmail(email: String): User
fun addStorageUsage(userId: Long, bytes: Long)   // 업로드 시 호출
fun subtractStorageUsage(userId: Long, bytes: Long)  // 삭제 시 호출
fun checkStorageAvailable(userId: Long, bytes: Long) // 초과 시 DriveException.StorageLimitExceeded
```

## Spring Security 연동

`UserService`가 `UserDetailsService`를 구현한다.

```kotlin
@Service
class UserService(...) : UserDetailsService {
    override fun loadUserByUsername(email: String): UserDetails {
        val user = userRepository.findByEmail(email)
            ?: throw UsernameNotFoundException("사용자를 찾을 수 없습니다.")
        return UserPrincipal(user)
    }
}
```

`UserPrincipal`은 `UserDetails`를 구현하며 `user.id`를 노출해 `currentUserId()` 확장함수에서 사용한다.

## SecurityConfig 핵심 설정

```kotlin
@Bean
fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
    http
        .authorizeHttpRequests {
            it.requestMatchers("/auth/**", "/share/**", "/css/**", "/js/**", "/icons/**").permitAll()
            it.requestMatchers("/h2-console/**").permitAll()  // dev profile 조건부 처리
            it.anyRequest().authenticated()
        }
        .formLogin {
            it.loginPage("/auth/login")
            it.loginProcessingUrl("/auth/login")
            it.defaultSuccessUrl("/drive", true)
            it.failureUrl("/auth/login?error")
        }
        .logout {
            it.logoutUrl("/auth/logout")
            it.logoutSuccessUrl("/auth/login")
        }
        .rememberMe { it.key("drive-remember-me").tokenValiditySeconds(604800) }  // 7일
    return http.build()
}
```

## 규칙

- 비밀번호는 `BCryptPasswordEncoder`로만 인코딩. 평문 저장 절대 금지.
- 회원가입 시 이메일 중복 확인 필수 → `DriveException.InvalidRequest("이미 사용 중인 이메일입니다.")`
- `storage_used_bytes` 변경은 반드시 `UserService.addStorageUsage` / `subtractStorageUsage`를 통해서만 수행. 직접 갱신 금지.
- 스토리지 초과 시 업로드를 즉시 차단하고 `DriveException.StorageLimitExceeded` 던지기.
