# 테스트 가이드

## 계층별 전략

| 계층 | 방식 | 베이스 클래스 | Spring 컨텍스트 |
|------|------|-------------|----------------|
| Controller | Spring 통합 | `IntegrationTestBase` 상속 | 공유 (한 번만 로드) |
| Service | Fake 의존성 | 없음 (순수 Kotlin) | 없음 |
| Infrastructure | JPA 어댑터 | `DataJpaTestBase` 상속 | 공유 (IntegrationTestBase와 동일) |

> **Mock 사용 금지** — 외부 시스템(이메일, S3 등)만 예외.  
> Service는 `src/test/.../fake/` 에 위치한 인메모리 Fake 구현체를 사용한다.

---

## Spring 컨텍스트 공유

`IntegrationTestBase`를 상속하는 모든 테스트 클래스는 **동일한 Spring 컨텍스트를 재사용**한다.  
컨텍스트는 최초 1회만 로드되어 전체 테스트 스위트에서 공유된다.

```
IntegrationTestBase (@SpringBootTest, @ActiveProfiles("test"), @Transactional)
├── AuthControllerTest
├── FileControllerTest
└── DataJpaTestBase
    ├── UserJpaRepositoryTest
    └── FileJpaRepositoryTest
```

**컨텍스트 재사용 조건**: 어노테이션 설정(`@SpringBootTest` + `@ActiveProfiles("test")`)이 동일해야 한다.  
베이스 클래스를 벗어나 별도 설정을 추가하면 새 컨텍스트가 생성된다.

---

## Controller 테스트

```kotlin
class SomeControllerTest @Autowired constructor(
    private val someService: SomeService,   // 실제 빈 주입
) : IntegrationTestBase() {

    @Test
    fun `POST 요청 — CSRF 필수`() {
        mockMvc.post("/path") {
            param("key", "value")
            with(csrf())           // ← form POST에 필수
        }.andExpect {
            status { is3xxRedirection() }
        }
    }

    @Test
    fun `인증 사용자 요청`() {
        mockMvc.get("/protected") {
            with(user("email@test.com").roles("USER"))
        }.andExpect { status { isOk() } }
    }
}
```

---

## Service 테스트 (Fake 패턴)

```kotlin
class UserServiceTest {
    private val userRepository = FakeUserRepository()
    private val userService = UserService(userRepository, BCryptPasswordEncoder())

    @BeforeEach fun setUp() { userRepository.clear() }

    @Test
    fun `예외 errorCode 검증`() {
        userService.register("a@b.com", "pw", "이름")

        assertThatThrownBy { userService.register("a@b.com", "pw2", "이름2") }
            .isInstanceOf(DriveException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", DriveErrorCode.USER_EMAIL_DUPLICATE)
    }
}
```

Fake 구현체 위치: `src/test/kotlin/demo/drive/{domain}/fake/Fake{Repository}.kt`

---

## Infrastructure 테스트

```kotlin
class UserJpaRepositoryTest @Autowired constructor(
    private val userJpaRepository: UserJpaRepository,
) : DataJpaTestBase() {

    @Test
    fun `이메일로 사용자 조회`() {
        userJpaRepository.save(User(email = "a@b.com", password = "hash", name = "이름"))
        val found = userJpaRepository.findByEmail("a@b.com")
        assertThat(found).isNotNull()
    }
}
```

---

## 공통 규칙

| 규칙 | 내용 |
|------|------|
| 트랜잭션 | `@Transactional` — 테스트 후 자동 롤백 (IntegrationTestBase 기본 적용) |
| 프로파일 | `test` 프로파일 (`src/test/resources/application-test.yaml`) |
| CSRF | form POST 요청에 `with(csrf())` 필수 |
| 예외 검증 | `.hasFieldOrPropertyWithValue("errorCode", DriveErrorCode.XXX)` |
| URL 패턴 | `redirectedUrlPattern("/path*")` — Ant 스타일 |
| 테스트명 | 한글 backtick으로 의도 명확히 |
