package demo.drive.group.controller

import demo.drive.group.service.GroupService
import demo.drive.support.IntegrationTestBase
import demo.drive.user.service.UserService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

class GroupControllerTest @Autowired constructor(
    private val userService: UserService,
    private val groupService: GroupService,
) : IntegrationTestBase() {

    private val testEmail = "group_ctrl@test.com"
    private var userId: Long = 0L

    private fun authAs(email: String): org.springframework.test.web.servlet.request.RequestPostProcessor {
        val principal = userService.loadUserByUsername(email)
        val auth = UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
        return authentication(auth)
    }

    @BeforeEach
    fun setup() {
        try { userService.register(testEmail, "password", "그룹테스터") } catch (_: Exception) {}
        userId = userService.findByEmail(testEmail).id
    }

    @Test
    fun `그룹 목록 페이지 - 인증 없으면 리다이렉트`() {
        mockMvc.get("/groups").andExpect { status { is3xxRedirection() } }
    }

    @Test
    fun `그룹 목록 페이지 - 인증 사용자 200`() {
        mockMvc.get("/groups") {
            with(authAs(testEmail))
        }.andExpect { status { isOk() } }
    }

    @Test
    fun `그룹 생성 API - 성공`() {
        mockMvc.post("/api/groups") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"새그룹","description":"설명"}"""
            with(authAs(testEmail))
            with(csrf())
        }.andExpect {
            status { isCreated() }
            jsonPath("$.data.name") { value("새그룹") }
        }
    }

    @Test
    fun `그룹 멤버 조회 API`() {
        val group = groupService.createGroup("멤버조회그룹", null, userId)

        mockMvc.get("/api/groups/${group.id}/members") {
            with(authAs(testEmail))
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.length()") { value(1) }
        }
    }

    @Test
    fun `그룹 해산 API - OWNER 성공`() {
        val group = groupService.createGroup("해산그룹", null, userId)

        mockMvc.delete("/api/groups/${group.id}") {
            with(authAs(testEmail))
            with(csrf())
        }.andExpect { status { isNoContent() } }
    }

    @Test
    fun `미인증 사용자 그룹 생성 불가`() {
        mockMvc.post("/api/groups") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"비인증그룹"}"""
            with(csrf())
        }.andExpect { status { is3xxRedirection() } }
    }
}
