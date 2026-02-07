package com.familyreunion.rsvp.config

import com.familyreunion.rsvp.model.AdminUser
import com.familyreunion.rsvp.repository.AdminUserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("!test")
class AdminInitializer(
    private val adminUserRepository: AdminUserRepository,
    @Value("\${INITIAL_ADMIN_EMAIL:}") private val initialEmail: String,
    @Value("\${INITIAL_ADMIN_NAME:Admin}") private val initialName: String
) : ApplicationRunner {

    override fun run(args: ApplicationArguments?) {
        if (initialEmail.isBlank()) return
        if (adminUserRepository.existsByEmail(initialEmail)) return

        adminUserRepository.save(AdminUser(email = initialEmail, name = initialName))
        println("Seeded initial admin: $initialEmail")
    }
}
