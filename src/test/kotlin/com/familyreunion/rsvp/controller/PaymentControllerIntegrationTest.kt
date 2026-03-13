package com.familyreunion.rsvp.controller

import com.familyreunion.rsvp.dto.RsvpRequest
import com.familyreunion.rsvp.model.AgeGroup
import com.familyreunion.rsvp.dto.AttendeeDto
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = ["ADMIN"])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PaymentControllerIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper
) {

    private fun createRsvp(familyName: String, adults: Int = 2, children: Int = 1): Long {
        val attendees = mutableListOf<AttendeeDto>()
        repeat(adults) { i ->
            attendees.add(AttendeeDto(guestName = "$familyName Adult ${i + 1}", guestAgeGroup = AgeGroup.ADULT))
        }
        repeat(children) { i ->
            attendees.add(AttendeeDto(guestName = "$familyName Child ${i + 1}", guestAgeGroup = AgeGroup.CHILD))
        }

        val request = RsvpRequest(
            familyName = familyName,
            headOfHouseholdName = "$familyName Head",
            email = "${familyName.lowercase()}@example.com",
            attendees = attendees,
            needsLodging = false
        )

        val response = mockMvc.perform(
            post("/api/rsvp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andReturn().response.contentAsString

        return objectMapper.readTree(response).get("id").asLong()
    }

    // --- Payment Summary contract tests ---

    @Test
    fun `GET summary returns array matching frontend PaymentSummaryResponse type`() {
        createRsvp("Tumblin")

        mockMvc.perform(get("/api/payments/summary"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$", hasSize<Any>(1)))
            .andExpect(jsonPath("$[0].rsvpId").isNumber)
            .andExpect(jsonPath("$[0].familyName").isString)
            .andExpect(jsonPath("$[0].totalOwed").isNumber)
            .andExpect(jsonPath("$[0].totalPaid").isNumber)
            .andExpect(jsonPath("$[0].balance").isNumber)
            .andExpect(jsonPath("$[0].status").isString)
            .andExpect(jsonPath("$[0].payments").isArray)
    }

    @Test
    fun `GET summary calculates correct amounts for adults and children`() {
        // 2 adults ($100 each) + 1 child ($50) = $250
        createRsvp("Smith", adults = 2, children = 1)

        mockMvc.perform(get("/api/payments/summary"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].familyName").value("Smith"))
            .andExpect(jsonPath("$[0].totalOwed").value(250.0))
            .andExpect(jsonPath("$[0].totalPaid").value(0.0))
            .andExpect(jsonPath("$[0].balance").value(250.0))
            .andExpect(jsonPath("$[0].status").value("UNPAID"))
    }

    @Test
    fun `GET summary by rsvpId matches frontend PaymentSummaryResponse type`() {
        val rsvpId = createRsvp("Jones")

        mockMvc.perform(get("/api/payments/summary/$rsvpId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.rsvpId").value(rsvpId))
            .andExpect(jsonPath("$.familyName").value("Jones"))
            .andExpect(jsonPath("$.totalOwed").isNumber)
            .andExpect(jsonPath("$.totalPaid").isNumber)
            .andExpect(jsonPath("$.balance").isNumber)
            .andExpect(jsonPath("$.status").value("UNPAID"))
            .andExpect(jsonPath("$.payments").isArray)
            .andExpect(jsonPath("$.payments", hasSize<Any>(0)))
    }

    @Test
    fun `GET summary returns 404 for unknown rsvpId`() {
        mockMvc.perform(get("/api/payments/summary/999"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `GET summary returns empty array when no RSVPs exist`() {
        mockMvc.perform(get("/api/payments/summary"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$", hasSize<Any>(0)))
    }

    @Test
    fun `GET summary returns correct status UNPAID when no payments`() {
        createRsvp("Williams")

        mockMvc.perform(get("/api/payments/summary"))
            .andExpect(jsonPath("$[0].status").value("UNPAID"))
            .andExpect(jsonPath("$[0].payments", hasSize<Any>(0)))
    }

    @Test
    fun `GET summary includes spouse fee at adult rate`() {
        val attendees = listOf(
            AttendeeDto(guestName = "Head", guestAgeGroup = AgeGroup.ADULT),
            AttendeeDto(guestName = "Spouse", guestAgeGroup = AgeGroup.SPOUSE)
        )
        val request = RsvpRequest(
            familyName = "Couple",
            headOfHouseholdName = "Head",
            email = "couple@example.com",
            attendees = attendees,
            needsLodging = false
        )
        mockMvc.perform(
            post("/api/rsvp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )

        // Adult $100 + Spouse $100 = $200
        mockMvc.perform(get("/api/payments/summary"))
            .andExpect(jsonPath("$[0].totalOwed").value(200.0))
    }

    @Test
    fun `GET summary infant fee is included`() {
        val attendees = listOf(
            AttendeeDto(guestName = "Parent", guestAgeGroup = AgeGroup.ADULT),
            AttendeeDto(guestName = "Baby", guestAgeGroup = AgeGroup.INFANT)
        )
        val request = RsvpRequest(
            familyName = "WithBaby",
            headOfHouseholdName = "Parent",
            email = "baby@example.com",
            attendees = attendees,
            needsLodging = false
        )
        mockMvc.perform(
            post("/api/rsvp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )

        // Adult $100 + Infant $15 = $115
        mockMvc.perform(get("/api/payments/summary"))
            .andExpect(jsonPath("$[0].totalOwed").value(115.0))
    }

    // --- Checkout contract tests ---

    @Test
    fun `POST checkout request body matches frontend CheckoutRequest type`() {
        val rsvpId = createRsvp("CheckoutTest")
        val json = """{"rsvpId":$rsvpId,"amount":10000}"""

        // Stripe not configured in test — endpoint returns error about missing key
        // The important contract check: the DTO deserialized (error mentions Stripe, not validation)
        val result = mockMvc.perform(
            post("/api/payments/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
        ).andReturn()

        val body = result.response.contentAsString
        assert(body.contains("Stripe")) {
            "Expected Stripe config error but got: $body (status ${result.response.status})"
        }
    }

    @Test
    fun `POST checkout validates minimum amount`() {
        val rsvpId = createRsvp("MinAmount")
        val json = """{"rsvpId":$rsvpId,"amount":50}"""

        mockMvc.perform(
            post("/api/payments/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
        )
            .andExpect(status().isBadRequest)
    }
}
