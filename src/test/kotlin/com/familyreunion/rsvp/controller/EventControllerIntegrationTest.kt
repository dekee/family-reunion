package com.familyreunion.rsvp.controller

import com.familyreunion.rsvp.dto.EventRegisterRequest
import com.familyreunion.rsvp.dto.EventRequest
import com.familyreunion.rsvp.dto.FamilyMemberRequest
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
class EventControllerIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper
) {

    private fun sampleEvent(
        title: String = "Family BBQ",
        dateTime: LocalDateTime = LocalDateTime.of(2026, 7, 4, 12, 0, 0)
    ) = EventRequest(
        title = title,
        description = "Annual cookout",
        eventDateTime = dateTime,
        address = "123 Park Ave",
        hostName = "Uncle Steve",
        notes = "Bring a dish"
    )

    private fun postEvent(request: EventRequest): String {
        return mockMvc.perform(
            post("/api/events")
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
    fun `POST response matches frontend EventResponse type`() {
        mockMvc.perform(
            post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleEvent()))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").isNumber)
            .andExpect(jsonPath("$.title").isString)
            .andExpect(jsonPath("$.description").isString)
            .andExpect(jsonPath("$.eventDateTime").isString)
            .andExpect(jsonPath("$.address").isString)
            .andExpect(jsonPath("$.hostName").isString)
            .andExpect(jsonPath("$.notes").isString)
            .andExpect(jsonPath("$.registrations").isArray)
            .andExpect(jsonPath("$.registrationCount").isNumber)
    }

    @Test
    fun `POST eventDateTime format includes seconds for frontend parsing`() {
        mockMvc.perform(
            post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleEvent()))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.eventDateTime").value(matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}")))
    }

    @Test
    fun `POST should create event with correct values`() {
        mockMvc.perform(
            post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleEvent()))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.title").value("Family BBQ"))
            .andExpect(jsonPath("$.description").value("Annual cookout"))
            .andExpect(jsonPath("$.address").value("123 Park Ave"))
            .andExpect(jsonPath("$.hostName").value("Uncle Steve"))
            .andExpect(jsonPath("$.notes").value("Bring a dish"))
            .andExpect(jsonPath("$.registrations", hasSize<Any>(0)))
            .andExpect(jsonPath("$.registrationCount").value(0))
    }

    @Test
    fun `POST should return 400 when title is blank`() {
        val invalid = sampleEvent().copy(title = "")
        mockMvc.perform(
            post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalid))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `POST should return 400 when address is blank`() {
        val invalid = sampleEvent().copy(address = "")
        mockMvc.perform(
            post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalid))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `POST should accept datetime without seconds from frontend`() {
        val json = """{"title":"Test","eventDateTime":"2026-07-04T12:00:00","address":"123 Main St"}"""
        mockMvc.perform(
            post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.eventDateTime").value("2026-07-04T12:00:00"))
    }

    // --- GET all ---

    @Test
    fun `GET should return all events as array`() {
        postEvent(sampleEvent("BBQ"))
        postEvent(sampleEvent("Pool Party"))

        mockMvc.perform(get("/api/events"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$", hasSize<Any>(2)))
    }

    @Test
    fun `GET empty list returns empty array`() {
        mockMvc.perform(get("/api/events"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$", hasSize<Any>(0)))
    }

    // --- PUT ---

    @Test
    fun `PUT should update event and return updated response`() {
        val responseJson = postEvent(sampleEvent())
        val id = objectMapper.readTree(responseJson).get("id").asLong()

        val updateRequest = sampleEvent().copy(title = "Updated BBQ", notes = "New notes")

        mockMvc.perform(
            put("/api/events/$id")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.title").value("Updated BBQ"))
            .andExpect(jsonPath("$.notes").value("New notes"))
    }

    // --- DELETE ---

    @Test
    fun `DELETE should remove event and return 204`() {
        val responseJson = postEvent(sampleEvent())
        val id = objectMapper.readTree(responseJson).get("id").asLong()

        mockMvc.perform(delete("/api/events/$id"))
            .andExpect(status().isNoContent)

        mockMvc.perform(get("/api/events"))
            .andExpect(jsonPath("$", hasSize<Any>(0)))
    }

    // --- Registration contract tests ---

    @Test
    fun `POST register returns event with registration matching frontend shape`() {
        val eventJson = postEvent(sampleEvent())
        val eventId = objectMapper.readTree(eventJson).get("id").asLong()

        val memberId = createMember("John Tumblin")

        mockMvc.perform(
            post("/api/events/$eventId/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(EventRegisterRequest(listOf(memberId))))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.registrations", hasSize<Any>(1)))
            .andExpect(jsonPath("$.registrations[0].id").isNumber)
            .andExpect(jsonPath("$.registrations[0].familyMemberId").isNumber)
            .andExpect(jsonPath("$.registrations[0].familyMemberName").isString)
            .andExpect(jsonPath("$.registrations[0].familyMemberName").value("John Tumblin"))
            .andExpect(jsonPath("$.registrationCount").value(1))
    }

    @Test
    fun `POST register multiple members increments registrationCount`() {
        val eventJson = postEvent(sampleEvent())
        val eventId = objectMapper.readTree(eventJson).get("id").asLong()

        val member1 = createMember("Alice")
        val member2 = createMember("Bob")

        mockMvc.perform(
            post("/api/events/$eventId/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(EventRegisterRequest(listOf(member1, member2))))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.registrations", hasSize<Any>(2)))
            .andExpect(jsonPath("$.registrationCount").value(2))
    }

    @Test
    fun `DELETE unregister removes member and returns 204`() {
        val eventJson = postEvent(sampleEvent())
        val eventId = objectMapper.readTree(eventJson).get("id").asLong()
        val memberId = createMember("Jane")

        mockMvc.perform(
            post("/api/events/$eventId/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(EventRegisterRequest(listOf(memberId))))
        )

        mockMvc.perform(delete("/api/events/$eventId/register/$memberId"))
            .andExpect(status().isNoContent)

        mockMvc.perform(get("/api/events"))
            .andExpect(jsonPath("$[0].registrationCount").value(0))
    }

    @Test
    fun `POST register with empty list returns 400`() {
        val eventJson = postEvent(sampleEvent())
        val eventId = objectMapper.readTree(eventJson).get("id").asLong()

        mockMvc.perform(
            post("/api/events/$eventId/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(EventRegisterRequest(emptyList())))
        )
            .andExpect(status().isBadRequest)
    }
}
