package demo.drive.user.domain

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class UserPrincipal(val user: User) : UserDetails {

    val id: Long get() = user.id

    override fun getAuthorities(): Collection<GrantedAuthority> =
        listOf(SimpleGrantedAuthority("ROLE_${user.systemRole.name}"))

    override fun getPassword(): String = user.password
    override fun getUsername(): String = user.email
}
