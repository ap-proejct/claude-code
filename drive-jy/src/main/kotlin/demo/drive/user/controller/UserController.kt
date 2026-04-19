package demo.drive.user.controller

import demo.drive.common.extension.currentUserId
import demo.drive.common.response.CommonResponse
import demo.drive.user.controller.dto.UpdateProfileRequest
import demo.drive.user.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*

@Controller
class UserController(private val userService: UserService) {

    @GetMapping("/profile")
    fun profile(model: Model): String {
        model.addAttribute("currentPath", "/profile")
        return "user/profile"
    }

    @PatchMapping("/api/users/me")
    @ResponseBody
    fun updateProfile(@RequestBody req: UpdateProfileRequest): ResponseEntity<CommonResponse<Map<String, Any>>> {
        val user = userService.updateProfile(currentUserId(), req.name)
        return CommonResponse.ok(mapOf("id" to user.id, "name" to user.name, "email" to user.email))
    }
}
