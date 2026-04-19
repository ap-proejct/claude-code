package demo.drive.share.controller

import demo.drive.file.service.FileService
import demo.drive.share.service.ShareService
import demo.drive.support.IntegrationTestBase
import demo.drive.user.service.UserService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

class ShareControllerTest @Autowired constructor(
    private val userService: UserService,
    private val fileService: FileService,
    private val shareService: ShareService,
) : IntegrationTestBase() {

    private val testEmail = "share_ctrl@test.com"
    private var userId: Long = 0L

    private fun authAs(email: String): org.springframework.test.web.servlet.request.RequestPostProcessor {
        val principal = userService.loadUserByUsername(email)
        val auth = UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
        return authentication(auth)
    }

    @BeforeEach
    fun setup() {
        try { userService.register(testEmail, "password", "공유테스터") } catch (_: Exception) {}
        userId = userService.findByEmail(testEmail).id
    }

    @Test
    fun `공유 링크 생성 - 소유자 성공`() {
        val file = fileService.createFolder("공유폴더", null, userId)

        mockMvc.post("/api/share/${file.id}") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"permission":"VIEWER"}"""
            with(authAs(testEmail))
            with(csrf())
        }.andExpect {
            status { isCreated() }
            jsonPath("$.success") { value(true) }
            jsonPath("$.data.token") { isString() }
            jsonPath("$.data.permission") { value("VIEWER") }
        }
    }

    @Test
    fun `공유 링크 취소`() {
        val file = fileService.createFolder("취소폴더", null, userId)
        val fp = shareService.createLink(file.id, userId, demo.drive.permission.domain.Permission.VIEWER, null)

        mockMvc.delete("/api/share/${file.id}/${fp.shareToken}") {
            with(authAs(testEmail))
            with(csrf())
        }.andExpect {
            status { isNoContent() }
        }
    }

    @Test
    fun `공유 링크 목록 조회`() {
        val file = fileService.createFolder("목록폴더", null, userId)
        shareService.createLink(file.id, userId, demo.drive.permission.domain.Permission.VIEWER, null)

        mockMvc.get("/api/share/${file.id}/links") {
            with(authAs(testEmail))
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.length()") { value(1) }
        }
    }

    @Test
    fun `공유 토큰으로 파일 정보 조회 - 인증 불필요`() {
        val file = fileService.createFolder("공개폴더", null, userId)
        val fp = shareService.createLink(file.id, userId, demo.drive.permission.domain.Permission.VIEWER, null)

        mockMvc.get("/api/share/resolve/${fp.shareToken}")
            .andExpect {
                status { isOk() }
                jsonPath("$.data.name") { value("공개폴더") }
            }
    }

    @Test
    fun `공유 페이지 인증 없이 접근 가능`() {
        val mockFile = MockMultipartFile("file", "shared.txt", "text/plain", "data".toByteArray())
        val file = fileService.upload(mockFile, null, userId)
        val fp = shareService.createLink(file.id, userId, demo.drive.permission.domain.Permission.VIEWER, null)

        mockMvc.get("/share/${fp.shareToken}")
            .andExpect { status { isOk() } }
    }

    @Test
    fun `잘못된 토큰으로 공유 페이지 접근 시 에러`() {
        mockMvc.get("/api/share/resolve/invalid-token-xyz")
            .andExpect { status { isNotFound() } }
    }

    @Test
    fun `미인증 사용자는 공유 링크 생성 불가`() {
        mockMvc.post("/api/share/1") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"permission":"VIEWER"}"""
            with(csrf())
        }.andExpect { status { is3xxRedirection() } }
    }
}
