package com.familyreunion.rsvp.controller

import com.familyreunion.rsvp.dto.MeetingRequest
import com.fasterxml.jackson.databind.ObjectMapper
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDateTime

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class MeetingControllerIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper
) {

    private fun sampleRequest(
        title: String = "Planning Call",
        dateTime: LocalDateTime = LocalDateTime.of(2026, 3, 15, 14, 0)
    ) = MeetingRequest(
        title = title,
        meetingDateTime = dateTime,
        zoomLink = "https://zoom.us/j/1234567890",
        phoneNumber = "+1 (312) 626-6799",
        meetingId = "123 456 7890",
        passcode = "reunion2026",
        notes = "Monthly planning call"
    )

    private fun postMeeting(request: MeetingRequest): String {
        return mockMvc.perform(
            post("/api/meetings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andReturn().response.contentAsString
    }

    // --- POST ---

    @Test
    fun `POST should create meeting and return 201`() {
        mockMvc.perform(
            post("/api/meetings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleRequest()))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").isNumber)
            .andExpect(jsonPath("$.title").value("Planning Call"))
            .andExpect(jsonPath("$.zoomLink").value("https://zoom.us/j/1234567890"))
            .andExpect(jsonPath("$.phoneNumber").value("+1 (312) 626-6799"))
            .andExpect(jsonPath("$.meetingId").value("123 456 7890"))
            .andExpect(jsonPath("$.passcode").value("reunion2026"))
    }

    @Test
    fun `POST should return 400 when title is blank`() {
        val invalid = sampleRequest().copy(title = "")

        mockMvc.perform(
            post("/api/meetings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalid))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `POST should return 400 when zoomLink is blank`() {
        val invalid = sampleRequest().copy(zoomLink = "")

        mockMvc.perform(
            post("/api/meetings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalid))
        )
            .andExpect(status().isBadRequest)
    }

    // --- GET all ---

    @Test
    fun `GET should return all meetings ordered by dateTime`() {
        postMeeting(sampleRequest("Later Call", LocalDateTime.of(2026, 6, 1, 10, 0)))
        postMeeting(sampleRequest("Earlier Call", LocalDateTime.of(2026, 3, 1, 10, 0)))

        mockMvc.perform(get("/api/meetings"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$", hasSize<Any>(2)))
            .andExpect(jsonPath("$[0].title").value("Earlier Call"))
            .andExpect(jsonPath("$[1].title").value("Later Call"))
    }

    // --- GET by id ---

    @Test
    fun `GET by id should return meeting`() {
        val responseJson = postMeeting(sampleRequest())
        val id = objectMapper.readTree(responseJson).get("id").asLong()

        mockMvc.perform(get("/api/meetings/$id"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.title").value("Planning Call"))
    }

    @Test
    fun `GET by id should return 404 when not found`() {
        mockMvc.perform(get("/api/meetings/999"))
            .andExpect(status().isNotFound)
    }

    // --- PUT ---

    @Test
    fun `PUT should update meeting and return 200`() {
        val responseJson = postMeeting(sampleRequest())
        val id = objectMapper.readTree(responseJson).get("id").asLong()

        val updateRequest = sampleRequest().copy(
            title = "Updated Planning Call",
            notes = "Rescheduled"
        )

        mockMvc.perform(
            put("/api/meetings/$id")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.title").value("Updated Planning Call"))
            .andExpect(jsonPath("$.notes").value("Rescheduled"))
    }

    @Test
    fun `PUT should return 404 when not found`() {
        mockMvc.perform(
            put("/api/meetings/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleRequest()))
        )
            .andExpect(status().isNotFound)
    }

    // --- DELETE ---

    @Test
    fun `DELETE should remove meeting and return 204`() {
        val responseJson = postMeeting(sampleRequest())
        val id = objectMapper.readTree(responseJson).get("id").asLong()

        mockMvc.perform(delete("/api/meetings/$id"))
            .andExpect(status().isNoContent)

        mockMvc.perform(get("/api/meetings/$id"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `DELETE should return 404 when not found`() {
        mockMvc.perform(delete("/api/meetings/999"))
            .andExpect(status().isNotFound)
    }
}
