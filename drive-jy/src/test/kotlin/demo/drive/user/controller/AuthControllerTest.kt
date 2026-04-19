package demo.drive.user.controller

import demo.drive.support.IntegrationTestBase
import demo.drive.user.service.UserService
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

class AuthControllerTest @Autowired constructor(
    private val userService: UserService,
) : IntegrationTestBase() {

    @Test
    fun `로그인 페이지 접근 가능`() {
        mockMvc.get("/auth/login")
            .andExpect {
                status { isOk() }
                content { string(containsString("로그인")) }
            }
    }

    @Test
    fun `회원가입 페이지 접근 가능`() {
        mockMvc.get("/auth/register")
            .andExpect {
                status { isOk() }
                content { string(containsString("회원가입")) }
            }
    }

    @Test
    fun `회원가입 성공 후 로그인 페이지로 리다이렉트`() {
        mockMvc.post("/auth/register") {
            param("email", "new@example.com")
            param("password", "password123")
            param("name", "신규유저")
            with(csrf())
        }.andExpect {
            status { is3xxRedirection() }
            redirectedUrl("/auth/login?registered")
        }
    }

    @Test
    fun `중복 이메일 회원가입 시 에러 메시지 표시`() {
        userService.register("dup@example.com", "password123", "기존유저")

        mockMvc.post("/auth/register") {
            param("email", "dup@example.com")
            param("password", "password456")
            param("name", "중복유저")
            with(csrf())
        }.andExpect {
            status { isOk() }
            content { string(containsString("이미 사용 중인 이메일")) }
        }
    }

    @Test
    fun `인증 없이 드라이브 접근 시 로그인 페이지로 리다이렉트`() {
        mockMvc.get("/drive")
            .andExpect {
                status { is3xxRedirection() }
                redirectedUrlPattern("/auth/login*")
            }
    }
}
