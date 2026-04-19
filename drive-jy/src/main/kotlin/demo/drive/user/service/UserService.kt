package demo.drive.user.service

import demo.drive.common.exception.DriveErrorCode
import demo.drive.common.exception.DriveException
import demo.drive.user.domain.User
import demo.drive.user.domain.UserPrincipal
import demo.drive.user.domain.UserRepository
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) : UserDetailsService {

    override fun loadUserByUsername(email: String): UserDetails {
        val user = userRepository.findByEmail(email)
            ?: throw UsernameNotFoundException("사용자를 찾을 수 없습니다.")
        return UserPrincipal(user)
    }

    fun register(email: String, password: String, name: String): User {
        if (userRepository.existsByEmail(email)) {
            throw DriveException(DriveErrorCode.USER_EMAIL_DUPLICATE)
        }
        val encoded = passwordEncoder.encode(password)
            ?: throw DriveException(DriveErrorCode.INVALID_REQUEST, "비밀번호를 처리할 수 없습니다.")
        return userRepository.save(User(email = email, password = encoded, name = name))
    }

    @Transactional(readOnly = true)
    fun findByEmail(email: String): User =
        userRepository.findByEmail(email) ?: throw DriveException(DriveErrorCode.USER_NOT_FOUND)

    @Transactional(readOnly = true)
    fun findById(userId: Long): User =
        userRepository.findById(userId) ?: throw DriveException(DriveErrorCode.USER_NOT_FOUND)

    fun addStorageUsage(userId: Long, bytes: Long) {
        val user = userRepository.findById(userId) ?: throw DriveException(DriveErrorCode.USER_NOT_FOUND)
        user.storageUsedBytes += bytes
        userRepository.save(user)
    }

    fun subtractStorageUsage(userId: Long, bytes: Long) {
        val user = userRepository.findById(userId) ?: throw DriveException(DriveErrorCode.USER_NOT_FOUND)
        user.storageUsedBytes = maxOf(0L, user.storageUsedBytes - bytes)
        userRepository.save(user)
    }

    fun updateProfile(userId: Long, name: String): User {
        if (name.isBlank()) throw DriveException(DriveErrorCode.INVALID_REQUEST, "이름을 입력해주세요.")
        val user = userRepository.findById(userId) ?: throw DriveException(DriveErrorCode.USER_NOT_FOUND)
        user.name = name.trim()
        return userRepository.save(user)
    }

    fun checkStorageAvailable(userId: Long, bytes: Long) {
        val user = userRepository.findById(userId) ?: throw DriveException(DriveErrorCode.USER_NOT_FOUND)
        if (user.storageUsedBytes + bytes > user.storageLimitBytes) {
            throw DriveException(DriveErrorCode.STORAGE_LIMIT_EXCEEDED)
        }
    }
}
