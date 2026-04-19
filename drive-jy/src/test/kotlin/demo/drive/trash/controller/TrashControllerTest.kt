package demo.drive.trash.controller

import demo.drive.file.service.FileService
import demo.drive.support.IntegrationTestBase
import demo.drive.trash.service.TrashService
import demo.drive.user.service.UserService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

class TrashControllerTest @Autowired constructor(
    private val userService: UserService,
    private val fileService: FileService,
    private val trashService: TrashService,
) : IntegrationTestBase() {

    private val testEmail = "trash_ctrl@test.com"
    private var userId: Long = 0L

    private fun authAs(email: String): org.springframework.test.web.servlet.request.RequestPostProcessor {
        val principal = userService.loadUserByUsername(email)
        val auth = UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
        return authentication(auth)
    }

    @BeforeEach
    fun createUser() {
        try {
            userService.register(testEmail, "password", "휴지통테스터")
        } catch (_: Exception) {}
        userId = userService.findByEmail(testEmail).id
    }

    @Test
    fun `trash 페이지 - 인증 없으면 로그인 페이지로 리다이렉트`() {
        mockMvc.get("/trash")
            .andExpect { status { is3xxRedirection() } }
    }

    @Test
    fun `api trash - 휴지통 목록 반환`() {
        mockMvc.get("/api/trash") {
            with(authAs(testEmail))
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `api trash restore - 파일 복구`() {
        val multipart = MockMultipartFile("file", "restore-test.txt", "text/plain", "data".toByteArray())
        val file = fileService.upload(multipart, null, userId)
        trashService.moveToTrash(file.id, userId)

        mockMvc.post("/api/trash/${file.id}/restore") {
            with(authAs(testEmail))
            with(csrf())
        }.andExpect {
            status { isNoContent() }
        }
    }

    @Test
    fun `api trash 영구 삭제`() {
        val multipart = MockMultipartFile("file", "perm-delete.txt", "text/plain", "data".toByteArray())
        val file = fileService.upload(multipart, null, userId)
        trashService.moveToTrash(file.id, userId)

        mockMvc.delete("/api/trash/${file.id}") {
            with(authAs(testEmail))
            with(csrf())
        }.andExpect {
            status { isNoContent() }
        }
    }

    @Test
    fun `api trash 전체 비우기`() {
        mockMvc.delete("/api/trash") {
            with(authAs(testEmail))
            with(csrf())
        }.andExpect {
            status { isNoContent() }
        }
    }
}
