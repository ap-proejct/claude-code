package demo.drive.file.controller

import demo.drive.file.service.FileService
import demo.drive.support.IntegrationTestBase
import demo.drive.user.service.UserService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.multipart
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import java.io.File as JavaFile

class FileControllerTest @Autowired constructor(
    private val fileService: FileService,
    private val userService: UserService,
    @Value("\${drive.storage.root:./build/test-storage}") private val storageRoot: String,
) : IntegrationTestBase() {

    private val testEmail = "filetest@test.com"

    private fun ensureUser() = try {
        userService.findByEmail(testEmail)
    } catch (e: Exception) {
        userService.register(testEmail, "password123", "파일테스터")
        userService.findByEmail(testEmail)
    }

    // 실제 UserPrincipal을 담은 Authentication 반환 (currentUserId() 정상 동작)
    private fun authAs(email: String): org.springframework.test.web.servlet.request.RequestPostProcessor {
        val principal = userService.loadUserByUsername(email)
        val auth = UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
        return authentication(auth)
    }

    @AfterEach
    fun cleanUpStorage() {
        JavaFile(storageRoot).deleteRecursively()
    }

    @Test
    fun `인증 없이 API 접근 시 리다이렉트`() {
        mockMvc.get("/api/files/1")
            .andExpect { status { is3xxRedirection() } }
    }

    @Test
    fun `폴더 생성 - 인증 사용자 성공`() {
        ensureUser()
        mockMvc.post("/api/files/folders") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"새 폴더"}"""
            with(csrf())
            with(authAs(testEmail))
        }.andExpect {
            status { isCreated() }
            jsonPath("$.success") { value(true) }
            jsonPath("$.data.name") { value("새 폴더") }
            jsonPath("$.data.type") { value("FOLDER") }
        }
    }

    @Test
    fun `파일 업로드 - 성공`() {
        ensureUser()
        val mockFile = MockMultipartFile("file", "hello.txt", "text/plain", "Hello".toByteArray())

        mockMvc.multipart("/api/files/upload") {
            file(mockFile)
            with(csrf())
            with(authAs(testEmail))
        }.andExpect {
            status { isCreated() }
            jsonPath("$.success") { value(true) }
            jsonPath("$.data.name") { value("hello.txt") }
            jsonPath("$.data.type") { value("FILE") }
        }
    }

    @Test
    fun `이름 변경 - 성공`() {
        val userObj = ensureUser()
        val folder = fileService.createFolder("원래이름", null, userObj.id)

        mockMvc.patch("/api/files/${folder.id}/rename") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"바뀐이름"}"""
            with(csrf())
            with(authAs(testEmail))
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.name") { value("바뀐이름") }
        }
    }

    @Test
    fun `휴지통 이동 - 성공`() {
        val userObj = ensureUser()
        val folder = fileService.createFolder("삭제폴더", null, userObj.id)

        mockMvc.delete("/api/files/${folder.id}") {
            with(csrf())
            with(authAs(testEmail))
        }.andExpect {
            status { isNoContent() }
        }
    }

    @Test
    fun `드라이브 메인 페이지 접근 - 성공`() {
        ensureUser()
        mockMvc.get("/drive") {
            with(authAs(testEmail))
        }.andExpect {
            status { isOk() }
        }
    }
}
