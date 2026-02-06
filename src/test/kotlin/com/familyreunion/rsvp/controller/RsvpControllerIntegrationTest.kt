package com.familyreunion.rsvp.controller

import com.familyreunion.rsvp.dto.AttendeeDto
import com.familyreunion.rsvp.dto.RsvpRequest
import com.familyreunion.rsvp.model.AgeGroup
import com.familyreunion.rsvp.model.FamilyMember
import com.familyreunion.rsvp.repository.FamilyMemberRepository
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
    private val objectMapper: ObjectMapper,
    private val familyMemberRepository: FamilyMemberRepository
) {

    private fun guestRequest(
        familyName: String = "Smith",
        needsLodging: Boolean = false
    ) = RsvpRequest(
        familyName = familyName,
        headOfHouseholdName = "$familyName Head",
        email = "${familyName.lowercase()}@test.com",
        phone = "555-0100",
        attendees = listOf(
            AttendeeDto(guestName = "Adult One", guestAgeGroup = AgeGroup.ADULT),
            AttendeeDto(guestName = "Child One", guestAgeGroup = AgeGroup.CHILD, dietaryNeeds = "gluten-free")
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
    fun `POST should create RSVP with guest attendees and return 201`() {
        mockMvc.perform(
            post("/api/rsvp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(guestRequest()))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").isNumber)
            .andExpect(jsonPath("$.familyName").value("Smith"))
            .andExpect(jsonPath("$.headOfHouseholdName").value("Smith Head"))
            .andExpect(jsonPath("$.email").value("smith@test.com"))
            .andExpect(jsonPath("$.attendees", hasSize<Any>(2)))
            .andExpect(jsonPath("$.attendees[0].guestName").value("Adult One"))
            .andExpect(jsonPath("$.needsLodging").value(false))
    }

    @Test
    fun `POST should create RSVP with family member attendee`() {
        val member = familyMemberRepository.save(
            FamilyMember(name = "Derrick", ageGroup = AgeGroup.ADULT, generation = 1)
        )

        val request = RsvpRequest(
            familyName = "Cheryl",
            headOfHouseholdName = "Cheryl Tumblin",
            email = "cheryl@tumblin.family",
            attendees = listOf(
                AttendeeDto(familyMemberId = member.id, dietaryNeeds = "vegan")
            )
        )

        mockMvc.perform(
            post("/api/rsvp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.attendees", hasSize<Any>(1)))
            .andExpect(jsonPath("$.attendees[0].familyMemberId").value(member.id))
            .andExpect(jsonPath("$.attendees[0].familyMemberName").value("Derrick"))
            .andExpect(jsonPath("$.attendees[0].dietaryNeeds").value("vegan"))
    }

    @Test
    fun `POST should return 400 when familyName is blank`() {
        val invalid = guestRequest().copy(familyName = "")

        mockMvc.perform(
            post("/api/rsvp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalid))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `POST should return 400 when attendees is empty`() {
        val invalid = guestRequest().copy(attendees = emptyList())

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
        postRsvp(guestRequest("Smith"))
        postRsvp(guestRequest("Johnson"))

        mockMvc.perform(get("/api/rsvp"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$", hasSize<Any>(2)))
    }

    // --- GET by id ---

    @Test
    fun `GET by id should return RSVP`() {
        val responseJson = postRsvp(guestRequest())
        val id = objectMapper.readTree(responseJson).get("id").asLong()

        mockMvc.perform(get("/api/rsvp/$id"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.familyName").value("Smith"))
            .andExpect(jsonPath("$.attendees", hasSize<Any>(2)))
    }

    @Test
    fun `GET by id should return 404 when not found`() {
        mockMvc.perform(get("/api/rsvp/999"))
            .andExpect(status().isNotFound)
    }

    // --- PUT ---

    @Test
    fun `PUT should update RSVP and return 200`() {
        val responseJson = postRsvp(guestRequest())
        val id = objectMapper.readTree(responseJson).get("id").asLong()

        val updateRequest = RsvpRequest(
            familyName = "Smith-Updated",
            headOfHouseholdName = "Jane Smith",
            email = "jane@smith.com",
            attendees = listOf(
                AttendeeDto(guestName = "Jane Smith", guestAgeGroup = AgeGroup.ADULT)
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
            .andExpect(jsonPath("$.attendees", hasSize<Any>(1)))
            .andExpect(jsonPath("$.needsLodging").value(true))
    }

    @Test
    fun `PUT should return 404 when not found`() {
        mockMvc.perform(
            put("/api/rsvp/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(guestRequest()))
        )
            .andExpect(status().isNotFound)
    }

    // --- DELETE ---

    @Test
    fun `DELETE should remove RSVP and return 204`() {
        val responseJson = postRsvp(guestRequest())
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
        postRsvp(guestRequest("Smith", needsLodging = true))
        postRsvp(guestRequest("Johnson", needsLodging = false))

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
