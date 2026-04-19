package demo.drive.user.controller

import demo.drive.common.exception.DriveException
import demo.drive.user.controller.dto.RegisterRequest
import demo.drive.user.service.UserService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/auth")
class AuthController(private val userService: UserService) {

    @GetMapping("/login")
    fun loginPage(): String = "auth/login"

    @GetMapping("/register")
    fun registerPage(model: Model): String {
        model.addAttribute("request", RegisterRequest())
        return "auth/register"
    }

    @PostMapping("/register")
    fun register(@ModelAttribute request: RegisterRequest, model: Model): String {
        return try {
            userService.register(request.email, request.password, request.name)
            "redirect:/auth/login?registered"
        } catch (e: DriveException) {
            model.addAttribute("error", e.message)
            model.addAttribute("request", request)
            "auth/register"
        }
    }
}
