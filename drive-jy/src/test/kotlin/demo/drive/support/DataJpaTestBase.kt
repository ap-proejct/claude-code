package demo.drive.support

/**
 * Infrastructure(JPA 어댑터) 테스트 베이스.
 *
 * Spring Boot 4.x에서는 @DataJpaTest 슬라이스 대신
 * IntegrationTestBase를 상속해 전체 컨텍스트를 재사용한다.
 * @Transactional이 IntegrationTestBase에 선언되어 테스트 후 롤백 보장.
 *
 * 사용:
 *   class UserJpaRepositoryTest : DataJpaTestBase() { ... }
 */
abstract class DataJpaTestBase : IntegrationTestBase()
