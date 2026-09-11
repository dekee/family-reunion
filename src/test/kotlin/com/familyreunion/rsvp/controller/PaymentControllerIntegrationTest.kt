package com.familyreunion.rsvp.controller

import com.familyreunion.rsvp.dto.RsvpRequest
import com.familyreunion.rsvp.model.AgeGroup
import com.familyreunion.rsvp.dto.AttendeeDto
import com.familyreunion.rsvp.model.Payment
import com.familyreunion.rsvp.model.PaymentLineItem
import com.familyreunion.rsvp.model.PaymentStatus
import com.familyreunion.rsvp.model.TshirtSize
import com.familyreunion.rsvp.repository.PaymentRepository
import com.familyreunion.rsvp.repository.RsvpRepository
import com.fasterxml.jackson.databind.ObjectMapper
import java.math.BigDecimal
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
    private val objectMapper: ObjectMapper,
    private val rsvpRepository: RsvpRepository,
    private val paymentRepository: PaymentRepository
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

    @Test
    fun `GET summary excludes angel contributions from totalPaid and balance`() {
        // 2 adults = $200 owed; $200 payment made up of one $100 adult fee + $100 angel donation
        val rsvpId = createRsvp("AngelFamily", adults = 2, children = 0)
        val rsvp = rsvpRepository.findById(rsvpId).get()

        val payment = Payment(
            rsvp = rsvp,
            amount = BigDecimal("200.00"),
            stripeSessionId = "sess_angel_test",
            status = PaymentStatus.COMPLETED
        )
        payment.lineItems.add(PaymentLineItem(
            payment = payment,
            guestName = "AngelFamily Adult 1",
            ageGroup = AgeGroup.ADULT,
            amount = BigDecimal("100.00")
        ))
        payment.lineItems.add(PaymentLineItem(
            payment = payment,
            guestName = "Angel Contribution",
            ageGroup = AgeGroup.ADULT,
            amount = BigDecimal("100.00")
        ))
        paymentRepository.save(payment)

        mockMvc.perform(get("/api/payments/summary/$rsvpId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalOwed").value(200.0))
            .andExpect(jsonPath("$.totalPaid").value(100.0))
            .andExpect(jsonPath("$.balance").value(100.0))
            .andExpect(jsonPath("$.status").value("PARTIAL"))
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

    // --- T-shirt sizes ---

    private data class SizedPayment(val payment: Payment, val member: PaymentLineItem, val guest: PaymentLineItem, val angel: PaymentLineItem)

    /** COMPLETED payment with one family member (ADULT, sized L), one CHILD guest (no size), and an angel donation. */
    private fun createSizedPayment(rsvpId: Long, status: PaymentStatus = PaymentStatus.COMPLETED): SizedPayment {
        val rsvp = rsvpRepository.findById(rsvpId).get()
        val payment = Payment(
            rsvp = rsvp,
            amount = BigDecimal("175.00"),
            stripeSessionId = "sess_sized_$rsvpId",
            status = status
        )
        val member = PaymentLineItem(payment = payment, familyMemberId = 42, familyMemberName = "Sized Adult",
            ageGroup = AgeGroup.ADULT, amount = BigDecimal("100.00"), tshirtSize = TshirtSize.L)
        val guest = PaymentLineItem(payment = payment, guestName = "Sized Kid",
            ageGroup = AgeGroup.CHILD, amount = BigDecimal("50.00"))
        val angel = PaymentLineItem(payment = payment, guestName = "Angel Contribution",
            ageGroup = AgeGroup.ADULT, amount = BigDecimal("25.00"))
        payment.lineItems.addAll(listOf(member, guest, angel))
        paymentRepository.save(payment)
        return SizedPayment(payment, member, guest, angel)
    }

    @Test
    fun `GET summary exposes paid members and guests with line item ids and sizes, excluding angel`() {
        val rsvpId = createRsvp("Sized", adults = 1, children = 1)
        val fx = createSizedPayment(rsvpId)

        mockMvc.perform(get("/api/payments/summary/$rsvpId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.paidMemberIds", contains(42)))
            .andExpect(jsonPath("$.paidMembers", hasSize<Any>(1)))
            .andExpect(jsonPath("$.paidMembers[0].memberId").value(42))
            .andExpect(jsonPath("$.paidMembers[0].lineItemId").value(fx.member.id))
            .andExpect(jsonPath("$.paidMembers[0].tshirtSize").value("L"))
            .andExpect(jsonPath("$.paidGuests", hasSize<Any>(1)))
            .andExpect(jsonPath("$.paidGuests[0].name").value("Sized Kid"))
            .andExpect(jsonPath("$.paidGuests[0].lineItemId").value(fx.guest.id))
            .andExpect(jsonPath("$.paidGuests[0].tshirtSize").value(nullValue()))
    }

    @Test
    fun `PUT line item size saves a valid size`() {
        val rsvpId = createRsvp("SizeSave", adults = 1, children = 1)
        val fx = createSizedPayment(rsvpId)

        mockMvc.perform(
            put("/api/payments/line-items/${fx.guest.id}/size")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"rsvpId":$rsvpId,"tshirtSize":"YL"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.lineItemId").value(fx.guest.id))
            .andExpect(jsonPath("$.tshirtSize").value("YL"))

        mockMvc.perform(get("/api/payments/summary/$rsvpId"))
            .andExpect(jsonPath("$.paidGuests[0].tshirtSize").value("YL"))
    }

    @Test
    fun `PUT line item size rejects a youth size for an adult`() {
        val rsvpId = createRsvp("SizeWrong", adults = 1, children = 1)
        val fx = createSizedPayment(rsvpId)

        mockMvc.perform(
            put("/api/payments/line-items/${fx.member.id}/size")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"rsvpId":$rsvpId,"tshirtSize":"YS"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error", containsString("not available")))

        mockMvc.perform(get("/api/payments/summary/$rsvpId"))
            .andExpect(jsonPath("$.paidMembers[0].tshirtSize").value("L"))
    }

    @Test
    fun `PUT line item size rejects the angel contribution`() {
        val rsvpId = createRsvp("SizeAngel", adults = 1, children = 1)
        val fx = createSizedPayment(rsvpId)

        mockMvc.perform(
            put("/api/payments/line-items/${fx.angel.id}/size")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"rsvpId":$rsvpId,"tshirtSize":"L"}""")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `PUT line item size returns 404 for a mismatched rsvpId or unknown line item`() {
        val rsvpId = createRsvp("SizeMismatch", adults = 1, children = 1)
        val otherRsvpId = createRsvp("SizeOther", adults = 1, children = 0)
        val fx = createSizedPayment(rsvpId)

        mockMvc.perform(
            put("/api/payments/line-items/${fx.guest.id}/size")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"rsvpId":$otherRsvpId,"tshirtSize":"YS"}""")
        )
            .andExpect(status().isNotFound)

        mockMvc.perform(
            put("/api/payments/line-items/999999/size")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"rsvpId":$rsvpId,"tshirtSize":"YS"}""")
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `PUT line item size rejects a pending payment`() {
        val rsvpId = createRsvp("SizePending", adults = 1, children = 1)
        val fx = createSizedPayment(rsvpId, status = PaymentStatus.PENDING)

        mockMvc.perform(
            put("/api/payments/line-items/${fx.guest.id}/size")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"rsvpId":$rsvpId,"tshirtSize":"YS"}""")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `POST checkout rejects a guest without a T-shirt size`() {
        val rsvpId = createRsvp("NoSize")
        val json = """{"rsvpId":$rsvpId,"amount":5000,"guests":[{"name":"Cousin","ageGroup":"CHILD","fee":5000}]}"""

        // Size validation runs before the Stripe check, so this fails on the size, not on Stripe config
        mockMvc.perform(
            post("/api/payments/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error", containsString("size is required for Cousin")))
    }

    @Test
    fun `POST checkout rejects an onesie size for a child guest`() {
        val rsvpId = createRsvp("WrongSize")
        val json = """{"rsvpId":$rsvpId,"amount":5000,"guests":[{"name":"Cousin","ageGroup":"CHILD","fee":5000,"tshirtSize":"NEWBORN"}]}"""

        mockMvc.perform(
            post("/api/payments/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error", containsString("not available for Cousin")))
    }

    @Test
    fun `GET history line items include lineItemId and tshirtSize`() {
        val rsvpId = createRsvp("HistorySize", adults = 1, children = 1)
        val fx = createSizedPayment(rsvpId)

        mockMvc.perform(get("/api/payments/history"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].lineItems[0].lineItemId").value(fx.member.id))
            .andExpect(jsonPath("$[0].lineItems[0].tshirtSize").value("L"))
            .andExpect(jsonPath("$[0].lineItems[1].tshirtSize").value(nullValue()))
    }
}
