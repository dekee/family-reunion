package com.familyreunion.rsvp.controller

import com.familyreunion.rsvp.dto.EventRequest
import com.familyreunion.rsvp.dto.FamilyMemberRequest
import com.familyreunion.rsvp.dto.VolunteerSignupRequest
import com.familyreunion.rsvp.dto.VolunteerTaskRequest
import com.familyreunion.rsvp.model.AgeGroup
import com.fasterxml.jackson.databind.ObjectMapper
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDateTime

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = ["ADMIN"])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class VolunteerTaskControllerIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper
) {

    private fun createEvent(title: String = "Fish Fry"): Long {
        val request = EventRequest(
            title = title,
            description = "Fish fry at Byron's",
            eventDateTime = LocalDateTime.of(2026, 10, 16, 18, 0, 0),
            address = "Byron's House",
            hostName = "Byron"
        )
        val response = mockMvc.perform(
            post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andReturn().response.contentAsString
        return objectMapper.readTree(response).get("id").asLong()
    }

    private fun sampleTask(eventId: Long, title: String = "Set up chairs") = VolunteerTaskRequest(
        title = title,
        description = "Help set up before the event",
        eventId = eventId
    )

    private fun postTask(request: VolunteerTaskRequest): String {
        return mockMvc.perform(
            post("/api/volunteer-tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andReturn().response.contentAsString
    }

    private fun createMember(name: String): Long {
        val request = FamilyMemberRequest(name = name, ageGroup = AgeGroup.ADULT)
        val response = mockMvc.perform(
            post("/api/family-tree/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andReturn().response.contentAsString
        return objectMapper.readTree(response).get("id").asLong()
    }

    // --- JSON shape contract tests ---

    @Test
    fun `POST response matches frontend VolunteerTaskResponse type`() {
        val eventId = createEvent()
        mockMvc.perform(
            post("/api/volunteer-tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleTask(eventId)))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").isNumber)
            .andExpect(jsonPath("$.title").isString)
            .andExpect(jsonPath("$.description").isString)
            .andExpect(jsonPath("$.eventId").value(eventId))
            .andExpect(jsonPath("$.eventTitle").value("Fish Fry"))
            .andExpect(jsonPath("$.eventDateTime").value(matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}")))
            .andExpect(jsonPath("$.signups").isArray)
            .andExpect(jsonPath("$.signupCount").value(0))
    }

    @Test
    fun `POST should return 400 when title is blank`() {
        val eventId = createEvent()
        mockMvc.perform(
            post("/api/volunteer-tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleTask(eventId).copy(title = "")))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `POST should return 404 when event does not exist`() {
        mockMvc.perform(
            post("/api/volunteer-tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleTask(999999)))
        )
            .andExpect(status().isNotFound)
    }

    // --- GET all ---

    @Test
    fun `GET should return all tasks as array`() {
        val eventId = createEvent()
        postTask(sampleTask(eventId, "Set up chairs"))
        postTask(sampleTask(eventId, "Clean up"))

        mockMvc.perform(get("/api/volunteer-tasks"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$", hasSize<Any>(2)))
    }

    @Test
    fun `GET empty list returns empty array`() {
        mockMvc.perform(get("/api/volunteer-tasks"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$", hasSize<Any>(0)))
    }

    // --- PUT ---

    @Test
    fun `PUT should update task and return updated response`() {
        val eventId = createEvent()
        val id = objectMapper.readTree(postTask(sampleTask(eventId))).get("id").asLong()

        val updateRequest = sampleTask(eventId).copy(title = "Break down tables", description = "After the party")

        mockMvc.perform(
            put("/api/volunteer-tasks/$id")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.title").value("Break down tables"))
            .andExpect(jsonPath("$.description").value("After the party"))
    }

    @Test
    fun `PUT should return 404 for unknown task`() {
        val eventId = createEvent()
        mockMvc.perform(
            put("/api/volunteer-tasks/999999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleTask(eventId)))
        )
            .andExpect(status().isNotFound)
    }

    // --- DELETE ---

    @Test
    fun `DELETE should remove task and return 204`() {
        val eventId = createEvent()
        val id = objectMapper.readTree(postTask(sampleTask(eventId))).get("id").asLong()

        mockMvc.perform(delete("/api/volunteer-tasks/$id"))
            .andExpect(status().isNoContent)

        mockMvc.perform(get("/api/volunteer-tasks"))
            .andExpect(jsonPath("$", hasSize<Any>(0)))
    }

    // --- Signup contract tests ---

    @Test
    fun `POST signup returns task with signup matching frontend shape`() {
        val eventId = createEvent()
        val taskId = objectMapper.readTree(postTask(sampleTask(eventId))).get("id").asLong()
        val memberId = createMember("John Tumblin")

        mockMvc.perform(
            post("/api/volunteer-tasks/$taskId/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(VolunteerSignupRequest(listOf(memberId))))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.signups", hasSize<Any>(1)))
            .andExpect(jsonPath("$.signups[0].id").isNumber)
            .andExpect(jsonPath("$.signups[0].familyMemberId").isNumber)
            .andExpect(jsonPath("$.signups[0].familyMemberName").value("John Tumblin"))
            .andExpect(jsonPath("$.signupCount").value(1))
    }

    @Test
    fun `POST signup multiple members increments signupCount`() {
        val eventId = createEvent()
        val taskId = objectMapper.readTree(postTask(sampleTask(eventId))).get("id").asLong()
        val member1 = createMember("Alice")
        val member2 = createMember("Bob")

        mockMvc.perform(
            post("/api/volunteer-tasks/$taskId/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(VolunteerSignupRequest(listOf(member1, member2))))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.signups", hasSize<Any>(2)))
            .andExpect(jsonPath("$.signupCount").value(2))
    }

    @Test
    fun `POST signup is idempotent for duplicate member`() {
        val eventId = createEvent()
        val taskId = objectMapper.readTree(postTask(sampleTask(eventId))).get("id").asLong()
        val memberId = createMember("Jane")
        val body = objectMapper.writeValueAsString(VolunteerSignupRequest(listOf(memberId)))

        mockMvc.perform(
            post("/api/volunteer-tasks/$taskId/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        ).andExpect(status().isOk)

        mockMvc.perform(
            post("/api/volunteer-tasks/$taskId/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.signupCount").value(1))
    }

    @Test
    fun `POST signup with empty list returns 400`() {
        val eventId = createEvent()
        val taskId = objectMapper.readTree(postTask(sampleTask(eventId))).get("id").asLong()

        mockMvc.perform(
            post("/api/volunteer-tasks/$taskId/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(VolunteerSignupRequest(emptyList())))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `POST signup with unknown member returns 404`() {
        val eventId = createEvent()
        val taskId = objectMapper.readTree(postTask(sampleTask(eventId))).get("id").asLong()

        mockMvc.perform(
            post("/api/volunteer-tasks/$taskId/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(VolunteerSignupRequest(listOf(999999))))
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `POST signup on unknown task returns 404`() {
        val memberId = createMember("Jane")
        mockMvc.perform(
            post("/api/volunteer-tasks/999999/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(VolunteerSignupRequest(listOf(memberId))))
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `DELETE signup removes member and returns 204`() {
        val eventId = createEvent()
        val taskId = objectMapper.readTree(postTask(sampleTask(eventId))).get("id").asLong()
        val memberId = createMember("Jane")

        mockMvc.perform(
            post("/api/volunteer-tasks/$taskId/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(VolunteerSignupRequest(listOf(memberId))))
        )

        mockMvc.perform(delete("/api/volunteer-tasks/$taskId/signup/$memberId"))
            .andExpect(status().isNoContent)

        mockMvc.perform(get("/api/volunteer-tasks"))
            .andExpect(jsonPath("$[0].signupCount").value(0))
    }

    // --- Cascade ---

    @Test
    fun `DELETE event removes its volunteer tasks`() {
        val eventId = createEvent()
        val taskId = objectMapper.readTree(postTask(sampleTask(eventId))).get("id").asLong()
        val memberId = createMember("Jane")

        mockMvc.perform(
            post("/api/volunteer-tasks/$taskId/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(VolunteerSignupRequest(listOf(memberId))))
        )

        mockMvc.perform(delete("/api/events/$eventId"))
            .andExpect(status().isNoContent)

        mockMvc.perform(get("/api/volunteer-tasks"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$", hasSize<Any>(0)))
    }
}
