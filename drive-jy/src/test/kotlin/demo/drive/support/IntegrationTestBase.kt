package demo.drive.support

import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext

/**
 * Controller 통합 테스트 베이스.
 *
 * Spring 컨텍스트는 동일한 설정(@SpringBootTest + @ActiveProfiles("test"))을
 * 공유하는 모든 하위 클래스에서 재사용된다 — 컨텍스트를 한 번만 로드.
 *
 * 사용:
 *   class MyControllerTest : IntegrationTestBase() { ... }
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
abstract class IntegrationTestBase {

    @Autowired
    private lateinit var wac: WebApplicationContext

    protected lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
            .apply<DefaultMockMvcBuilder>(springSecurity())
            .build()
    }
}
