package com.familyreunion.rsvp.controller

import com.familyreunion.rsvp.model.AdminUser
import com.familyreunion.rsvp.repository.AdminUserRepository
import com.familyreunion.rsvp.security.GoogleTokenVerifier
import com.familyreunion.rsvp.security.GoogleUserInfo
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SecurityIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val adminUserRepository: AdminUserRepository
) {

    @MockitoBean
    private lateinit var googleTokenVerifier: GoogleTokenVerifier

    private fun seedAdmin(email: String = "admin@test.com", name: String = "Admin") {
        adminUserRepository.save(AdminUser(email = email, name = name))
    }

    private fun setupMockToken(email: String = "admin@test.com", name: String = "Admin") {
        whenever(googleTokenVerifier.verify(any())).thenReturn(GoogleUserInfo(email, name))
    }

    // --- Public endpoints should work without auth ---

    @Test
    fun `GET endpoints should be public`() {
        mockMvc.perform(get("/api/rsvp"))
            .andExpect(status().isOk)
    }

    @Test
    fun `PUT rsvp should return 401 without auth`() {
        mockMvc.perform(
            put("/api/rsvp/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"familyName":"Test","headOfHouseholdName":"Test","email":"t@t.com","attendees":[{"guestName":"A","guestAgeGroup":"ADULT"}],"needsLodging":false}""")
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `POST rsvp should be public`() {
        mockMvc.perform(
            post("/api/rsvp")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"familyName":"Test","headOfHouseholdName":"Test","email":"t@t.com","attendees":[{"guestName":"A","guestAgeGroup":"ADULT"}],"needsLodging":false}""")
        )
            .andExpect(status().isCreated)
    }

    // --- Protected endpoints should deny anonymous users ---

    @Test
    fun `DELETE rsvp should return 401 without auth`() {
        mockMvc.perform(delete("/api/rsvp/1"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `POST meetings should return 401 without auth`() {
        mockMvc.perform(
            post("/api/meetings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"Test","meetingDateTime":"2026-03-15T14:00:00","zoomLink":"https://zoom.us/j/123"}""")
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `POST events should return 401 without auth`() {
        mockMvc.perform(
            post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"Test","eventDateTime":"2026-07-04T10:00:00","address":"123 Main St"}""")
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `POST family-tree members should return 401 without auth`() {
        mockMvc.perform(
            post("/api/family-tree/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Test","ageGroup":"ADULT"}""")
        )
            .andExpect(status().isUnauthorized)
    }

    // --- Protected endpoints should work with admin auth ---

    @Test
    fun `POST rsvp should succeed with admin token`() {
        seedAdmin()
        setupMockToken()

        mockMvc.perform(
            post("/api/rsvp")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"familyName":"Test","headOfHouseholdName":"Test","email":"t@t.com","attendees":[{"guestName":"A","guestAgeGroup":"ADULT"}],"needsLodging":false}""")
        )
            .andExpect(status().isCreated)
    }

    @Test
    fun `POST meetings should succeed with admin token`() {
        seedAdmin()
        setupMockToken()

        mockMvc.perform(
            post("/api/meetings")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"Test","meetingDateTime":"2026-03-15T14:00:00","zoomLink":"https://zoom.us/j/123"}""")
        )
            .andExpect(status().isCreated)
    }

    // --- Non-admin Google user should be rejected for admin endpoints ---

    @Test
    fun `PUT rsvp should return 401 for non-admin Google user`() {
        whenever(googleTokenVerifier.verify(any())).thenReturn(GoogleUserInfo("nobody@test.com", "Nobody"))

        mockMvc.perform(
            put("/api/rsvp/999")
                .header("Authorization", "Bearer some-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"familyName":"Test","headOfHouseholdName":"Test","email":"t@t.com","attendees":[{"guestName":"A","guestAgeGroup":"ADULT"}],"needsLodging":false}""")
        )
            .andExpect(status().isUnauthorized)
    }

    // --- Admin user management ---

    @Test
    fun `GET admin users should return 401 without auth`() {
        mockMvc.perform(get("/api/admin/users"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `GET admin users should succeed with admin token`() {
        seedAdmin()
        setupMockToken()

        mockMvc.perform(
            get("/api/admin/users")
                .header("Authorization", "Bearer valid-token")
        )
            .andExpect(status().isOk)
    }
}
