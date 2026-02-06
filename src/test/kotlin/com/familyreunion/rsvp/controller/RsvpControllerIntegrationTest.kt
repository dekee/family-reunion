package com.familyreunion.rsvp.controller

import com.familyreunion.rsvp.dto.FamilyMemberDto
import com.familyreunion.rsvp.dto.RsvpRequest
import com.familyreunion.rsvp.model.AgeGroup
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
import java.time.LocalDate

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RsvpControllerIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper
) {

    private fun validRequest(
        familyName: String = "Smith",
        needsLodging: Boolean = false
    ) = RsvpRequest(
        familyName = familyName,
        headOfHouseholdName = "$familyName Head",
        email = "${familyName.lowercase()}@test.com",
        phone = "555-0100",
        familyMembers = listOf(
            FamilyMemberDto(name = "Adult One", ageGroup = AgeGroup.ADULT),
            FamilyMemberDto(name = "Child One", ageGroup = AgeGroup.CHILD, dietaryNeeds = "gluten-free")
        ),
        needsLodging = needsLodging,
        arrivalDate = LocalDate.of(2026, 7, 4),
        departureDate = LocalDate.of(2026, 7, 6),
        notes = "Can't wait!"
    )

    private fun postRsvp(request: RsvpRequest): String {
        return mockMvc.perform(
            post("/api/rsvp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andReturn().response.contentAsString
    }

    // --- POST ---

    @Test
    fun `POST should create RSVP and return 201`() {
        mockMvc.perform(
            post("/api/rsvp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest()))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").isNumber)
            .andExpect(jsonPath("$.familyName").value("Smith"))
            .andExpect(jsonPath("$.headOfHouseholdName").value("Smith Head"))
            .andExpect(jsonPath("$.email").value("smith@test.com"))
            .andExpect(jsonPath("$.familyMembers", hasSize<Any>(2)))
            .andExpect(jsonPath("$.familyMembers[0].name").value("Adult One"))
            .andExpect(jsonPath("$.needsLodging").value(false))
    }

    @Test
    fun `POST should return 400 when familyName is blank`() {
        val invalid = validRequest().copy(familyName = "")

        mockMvc.perform(
            post("/api/rsvp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalid))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `POST should return 400 when familyMembers is empty`() {
        val invalid = validRequest().copy(familyMembers = emptyList())

        mockMvc.perform(
            post("/api/rsvp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalid))
        )
            .andExpect(status().isBadRequest)
    }

    // --- GET all ---

    @Test
    fun `GET should return all RSVPs`() {
        postRsvp(validRequest("Smith"))
        postRsvp(validRequest("Johnson"))

        mockMvc.perform(get("/api/rsvp"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$", hasSize<Any>(2)))
    }

    // --- GET by id ---

    @Test
    fun `GET by id should return RSVP`() {
        val responseJson = postRsvp(validRequest())
        val id = objectMapper.readTree(responseJson).get("id").asLong()

        mockMvc.perform(get("/api/rsvp/$id"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.familyName").value("Smith"))
            .andExpect(jsonPath("$.familyMembers", hasSize<Any>(2)))
    }

    @Test
    fun `GET by id should return 404 when not found`() {
        mockMvc.perform(get("/api/rsvp/999"))
            .andExpect(status().isNotFound)
    }

    // --- PUT ---

    @Test
    fun `PUT should update RSVP and return 200`() {
        val responseJson = postRsvp(validRequest())
        val id = objectMapper.readTree(responseJson).get("id").asLong()

        val updateRequest = RsvpRequest(
            familyName = "Smith-Updated",
            headOfHouseholdName = "Jane Smith",
            email = "jane@smith.com",
            familyMembers = listOf(
                FamilyMemberDto(name = "Jane Smith", ageGroup = AgeGroup.ADULT)
            ),
            needsLodging = true
        )

        mockMvc.perform(
            put("/api/rsvp/$id")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.familyName").value("Smith-Updated"))
            .andExpect(jsonPath("$.headOfHouseholdName").value("Jane Smith"))
            .andExpect(jsonPath("$.familyMembers", hasSize<Any>(1)))
            .andExpect(jsonPath("$.needsLodging").value(true))
    }

    @Test
    fun `PUT should return 404 when not found`() {
        mockMvc.perform(
            put("/api/rsvp/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest()))
        )
            .andExpect(status().isNotFound)
    }

    // --- DELETE ---

    @Test
    fun `DELETE should remove RSVP and return 204`() {
        val responseJson = postRsvp(validRequest())
        val id = objectMapper.readTree(responseJson).get("id").asLong()

        mockMvc.perform(delete("/api/rsvp/$id"))
            .andExpect(status().isNoContent)

        mockMvc.perform(get("/api/rsvp/$id"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `DELETE should return 404 when not found`() {
        mockMvc.perform(delete("/api/rsvp/999"))
            .andExpect(status().isNotFound)
    }

    // --- Summary ---

    @Test
    fun `GET summary should return correct budget counts`() {
        // Family 1: 1 adult, 1 child, needs lodging
        postRsvp(validRequest("Smith", needsLodging = true))

        // Family 2: 1 adult, 1 child, no lodging
        postRsvp(validRequest("Johnson", needsLodging = false))

        mockMvc.perform(get("/api/rsvp/summary"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalFamilies").value(2))
            .andExpect(jsonPath("$.totalHeadcount").value(4))
            .andExpect(jsonPath("$.adultCount").value(2))
            .andExpect(jsonPath("$.childCount").value(2))
            .andExpect(jsonPath("$.infantCount").value(0))
            .andExpect(jsonPath("$.lodgingCount").value(1))
    }
}
