package com.familyreunion.rsvp.controller

import com.familyreunion.rsvp.dto.RsvpRequest
import com.familyreunion.rsvp.model.AgeGroup
import com.familyreunion.rsvp.dto.AttendeeDto
import com.familyreunion.rsvp.model.Attendee
import com.familyreunion.rsvp.model.FamilyMember
import com.familyreunion.rsvp.model.Payment
import com.familyreunion.rsvp.model.LineItemKind
import com.familyreunion.rsvp.model.PaymentLineItem
import com.familyreunion.rsvp.model.PaymentStatus
import com.familyreunion.rsvp.model.TshirtSize
import com.familyreunion.rsvp.model.Rsvp
import com.familyreunion.rsvp.repository.FamilyMemberRepository
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
    private val paymentRepository: PaymentRepository,
    private val familyMemberRepository: FamilyMemberRepository
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
    fun `PUT line item size allows any size regardless of age group`() {
        // A small adult may need a youth shirt and a big kid an adult one
        val rsvpId = createRsvp("SizeAnyGroup", adults = 1, children = 1)
        val fx = createSizedPayment(rsvpId)

        mockMvc.perform(
            put("/api/payments/line-items/${fx.member.id}/size")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"rsvpId":$rsvpId,"tshirtSize":"YXL"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.tshirtSize").value("YXL"))

        mockMvc.perform(
            put("/api/payments/line-items/${fx.guest.id}/size")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"rsvpId":$rsvpId,"tshirtSize":"XL"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.tshirtSize").value("XL"))
    }

    @Test
    fun `PUT line item size rejects an unknown size`() {
        val rsvpId = createRsvp("SizeUnknown", adults = 1, children = 1)
        val fx = createSizedPayment(rsvpId)

        mockMvc.perform(
            put("/api/payments/line-items/${fx.member.id}/size")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"rsvpId":$rsvpId,"tshirtSize":"HUGE"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error", containsString("Unknown T-shirt size")))

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
    fun `POST checkout rejects an unknown size`() {
        val rsvpId = createRsvp("WrongSize")
        val json = """{"rsvpId":$rsvpId,"amount":5000,"guests":[{"name":"Cousin","ageGroup":"CHILD","fee":5000,"tshirtSize":"HUGE"}]}"""

        mockMvc.perform(
            post("/api/payments/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error", containsString("Unknown T-shirt size 'HUGE' for Cousin")))
    }

    @Test
    fun `POST checkout accepts an adult size for a child guest`() {
        val rsvpId = createRsvp("BigKid")
        val json = """{"rsvpId":$rsvpId,"amount":5000,"guests":[{"name":"Cousin","ageGroup":"CHILD","fee":5000,"tshirtSize":"L"}]}"""

        // Size validation passes; the request then fails on Stripe not being configured in tests
        val body = mockMvc.perform(
            post("/api/payments/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
        ).andReturn().response.contentAsString
        assert(body.contains("Stripe")) { "Expected Stripe config error but got: $body" }
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

    // --- Standalone Angel Fund gifts ---

    /** A COMPLETED gift with no RSVP behind it: exactly what a standalone donation persists. */
    private fun createStandaloneGift(
        sessionSuffix: String,
        amount: String = "50.00",
        donorName: String? = "Ada Tumblin",
        donorFamilyLabel: String? = "Norris",
        donorAnonymous: Boolean = false,
        payerName: String? = "Stripe Cardholder"
    ): Payment {
        val payment = Payment(
            rsvp = null,
            amount = BigDecimal(amount),
            stripeSessionId = "sess_gift_$sessionSuffix",
            status = PaymentStatus.COMPLETED,
            payerName = payerName,
            donorName = donorName,
            donorFamilyLabel = donorFamilyLabel,
            donorAnonymous = donorAnonymous
        )
        payment.lineItems.add(PaymentLineItem(payment = payment, guestName = "Angel Contribution",
            ageGroup = AgeGroup.ADULT, amount = BigDecimal(amount)))
        return paymentRepository.save(payment)
    }

    private fun donateJson(amountCents: Long, donorName: String? = "Ada", anonymous: Boolean = false): String {
        val name = donorName?.let { "\"$it\"" } ?: "null"
        return """{"amountCents":$amountCents,"donorName":$name,"familyLabel":"Norris","anonymous":$anonymous}"""
    }

    private fun postDonate(json: String, ip: String) = mockMvc.perform(
        post("/api/payments/donate")
            .header("X-Forwarded-For", ip)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json)
    )

    @Test
    fun `POST donate rejects an amount below the one dollar minimum`() {
        postDonate(donateJson(50), "10.0.0.1")
            .andExpect(status().isBadRequest)
            // Asserts the {"error":...} shape, which is the only one the frontend can display.
            .andExpect(jsonPath("$.error", containsString("Minimum donation is $1")))
    }

    @Test
    fun `POST donate rejects a zero or negative amount`() {
        postDonate(donateJson(0), "10.0.0.2").andExpect(status().isBadRequest)
        postDonate(donateJson(-500), "10.0.0.3").andExpect(status().isBadRequest)
    }

    @Test
    fun `POST donate rejects an amount above the cap`() {
        postDonate(donateJson(2_000_000), "10.0.0.4")
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error", containsString("capped")))
    }

    @Test
    fun `POST donate with a valid amount passes validation and reaches Stripe`() {
        // Stripe is unconfigured under the test profile, so this is the furthest the happy path
        // can go — which makes it the proof that the DTO bound and every check passed.
        postDonate(donateJson(2500), "10.0.0.5")
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error", containsString("Stripe")))
    }

    @Test
    fun `POST donate rejects an over-long donor name`() {
        postDonate(donateJson(2500, donorName = "x".repeat(200)), "10.0.0.6")
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errors.donorName").exists())
    }

    @Test
    fun `GET angels reports a standalone gift using the donor-supplied attribution`() {
        createStandaloneGift("named")

        mockMvc.perform(get("/api/payments/angels"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$", hasSize<Any>(1)))
            .andExpect(jsonPath("$[0].payerName").value("Ada Tumblin"))
            .andExpect(jsonPath("$[0].familyName").value("Norris"))
            .andExpect(jsonPath("$[0].amount").value(50.00))
    }

    @Test
    fun `GET angels hides the donor of an anonymous gift even though Stripe supplied a name`() {
        createStandaloneGift("anon", donorName = null, donorFamilyLabel = null,
            donorAnonymous = true, payerName = "Real Cardholder Name")

        mockMvc.perform(get("/api/payments/angels"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].payerName").value("Anonymous"))
            .andExpect(jsonPath("$[0].familyName").value(""))
    }

    @Test
    fun `GET angels falls back to the Stripe payer name for a legacy in-branch gift`() {
        val rsvpId = createRsvp("Legacy", adults = 1, children = 0)
        val fx = createSizedPayment(rsvpId)
        fx.payment.payerName = "Branch Payer"
        paymentRepository.save(fx.payment)

        mockMvc.perform(get("/api/payments/angels"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].payerName").value("Branch Payer"))
            .andExpect(jsonPath("$[0].familyName").value("Legacy"))
    }

    @Test
    fun `GET summary is unaffected by a standalone gift`() {
        val rsvpId = createRsvp("Untouched", adults = 1, children = 0)
        createStandaloneGift("untouched", amount = "500.00")

        // A gift has no RSVP, so it must never reach a family's balance.
        mockMvc.perform(get("/api/payments/summary/$rsvpId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalPaid").value(0))
            .andExpect(jsonPath("$.totalOwed").value(100.00))
            .andExpect(jsonPath("$.balance").value(100.00))
            .andExpect(jsonPath("$.status").value("UNPAID"))
    }

    @Test
    fun `GET history labels a standalone gift instead of calling it unknown`() {
        createStandaloneGift("history")

        mockMvc.perform(get("/api/payments/history"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].familyName").value("Angel Fund"))
            .andExpect(jsonPath("$[0].rsvpId").value(0))
            .andExpect(jsonPath("$[0].donationOnly").value(true))
            .andExpect(jsonPath("$[0].donorName").value("Ada Tumblin"))
    }

    @Test
    fun `PUT line item size is not usable on a standalone gift`() {
        val gift = createStandaloneGift("nosize")
        val angelId = gift.lineItems.first().id

        mockMvc.perform(
            put("/api/payments/line-items/$angelId/size")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"rsvpId":1,"tshirtSize":"L"}""")
        ).andExpect(status().isNotFound)
    }

    @Test
    fun `GET summary does not bill for a member excluded from the RSVP`() {
        // An excluded member is hidden from the pay page, so nobody can ever pay their fee.
        // Billing for them would leave a balance no one can clear.
        val included = familyMemberRepository.save(
            FamilyMember(name = "Billed Adult", ageGroup = AgeGroup.ADULT)
        )
        val excluded = familyMemberRepository.save(
            FamilyMember(name = "Excluded Adult", ageGroup = AgeGroup.ADULT, excludeFromRsvp = true)
        )
        val rsvp = Rsvp(
            familyName = "Excludes",
            headOfHouseholdName = "Excludes Head",
            email = "excludes@example.com"
        )
        rsvp.attendees.add(Attendee(rsvp = rsvp, familyMember = included))
        rsvp.attendees.add(Attendee(rsvp = rsvp, familyMember = excluded))
        val saved = rsvpRepository.save(rsvp)

        mockMvc.perform(get("/api/payments/summary/${saved.id}"))
            .andExpect(status().isOk)
            // $100 for the included adult only — not $200.
            .andExpect(jsonPath("$.totalOwed").value(100.00))
            .andExpect(jsonPath("$.balance").value(100.00))
    }

    // --- Pay-what-you-can donation checkout ---

    private data class MemberRsvp(val rsvpId: Long, val adultId: Long, val childId: Long)

    /**
     * An RSVP whose attendees are real family members. The guest-based [createRsvp] cannot be used
     * for contribution tests: with no familyMember behind an attendee, every memberId fails the
     * ownership guard before any other rule is reached.
     */
    private fun createMemberRsvp(familyName: String): MemberRsvp {
        val adult = familyMemberRepository.save(
            FamilyMember(name = "$familyName Adult", ageGroup = AgeGroup.ADULT)
        )
        val child = familyMemberRepository.save(
            FamilyMember(name = "$familyName Child", ageGroup = AgeGroup.CHILD)
        )
        val rsvp = Rsvp(
            familyName = familyName,
            headOfHouseholdName = "$familyName Head",
            email = "${familyName.lowercase()}@example.com"
        )
        rsvp.attendees.add(Attendee(rsvp = rsvp, familyMember = adult))
        rsvp.attendees.add(Attendee(rsvp = rsvp, familyMember = child))
        val saved = rsvpRepository.save(rsvp)
        return MemberRsvp(saved.id, adult.id, child.id)
    }

    private fun attendeeJson(memberId: Long, size: String? = null): String {
        val sizeField = size?.let { ""","tshirtSize":"$it"""" } ?: ""
        return """{"memberId":$memberId,"wantsShirt":${size != null}$sizeField}"""
    }

    private fun guestJson(name: String, ageGroup: String = "ADULT", size: String? = null): String {
        val sizeField = size?.let { ""","tshirtSize":"$it"""" } ?: ""
        return """{"guestName":"$name","ageGroup":"$ageGroup","wantsShirt":${size != null}$sizeField}"""
    }

    private fun contributeJson(
        rsvpId: Long,
        amount: Long,
        donationCents: Long,
        vararg attendees: String
    ) = """{"rsvpId":$rsvpId,"amount":$amount,"donationCents":$donationCents,"attendees":[${attendees.joinToString(",")}]}"""

    private fun postContribute(json: String, ip: String) = mockMvc.perform(
        post("/api/payments/contribute")
            .header("X-Forwarded-For", ip)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json)
    )

    @Test
    fun `POST contribute accepts a donation plus shirts and reaches Stripe`() {
        val fx = createMemberRsvp("GiveBoth")
        // $40 gift + one $15 shirt = $55. Stripe is unconfigured under the test profile, so this is
        // the furthest the happy path can go — which makes it the proof every rule above passed.
        postContribute(
            contributeJson(fx.rsvpId, 5500, 4000, attendeeJson(fx.adultId, "L"), attendeeJson(fx.childId)),
            "10.1.0.1"
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error", containsString("Stripe")))
    }

    @Test
    fun `POST contribute accepts shirts with no donation at all`() {
        val fx = createMemberRsvp("GiveShirtsOnly")
        postContribute(
            contributeJson(fx.rsvpId, 3000, 0, attendeeJson(fx.adultId, "L"), attendeeJson(fx.childId, "YM")),
            "10.1.0.2"
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error", containsString("Stripe")))
    }

    @Test
    fun `POST contribute accepts a donation with nobody taking a shirt`() {
        val fx = createMemberRsvp("GiveNoShirts")
        postContribute(
            contributeJson(fx.rsvpId, 2000, 2000, attendeeJson(fx.adultId)),
            "10.1.0.3"
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error", containsString("Stripe")))
    }

    @Test
    fun `POST contribute rejects a total that does not match the server price`() {
        val fx = createMemberRsvp("GiveTamper")
        // Claims a $5 shirt. The gift half is taken as given, but the shirt half is repriced here.
        postContribute(
            contributeJson(fx.rsvpId, 4500, 4000, attendeeJson(fx.adultId, "L")),
            "10.1.0.4"
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error", containsString("Amount mismatch")))
    }

    @Test
    fun `POST contribute rejects an empty attendee list`() {
        val fx = createMemberRsvp("GiveNobody")
        postContribute(contributeJson(fx.rsvpId, 4000, 4000), "10.1.0.5")
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error", containsString("at least one person")))
    }

    @Test
    fun `POST contribute rejects a member from another family`() {
        val fx = createMemberRsvp("GiveMine")
        val other = createMemberRsvp("GiveTheirs")
        postContribute(
            contributeJson(fx.rsvpId, 1500, 0, attendeeJson(other.adultId, "L")),
            "10.1.0.6"
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error", containsString("do not belong to RSVP")))
    }

    @Test
    fun `POST contribute rejects the same member twice`() {
        val fx = createMemberRsvp("GiveTwice")
        postContribute(
            contributeJson(fx.rsvpId, 3000, 0, attendeeJson(fx.adultId, "L"), attendeeJson(fx.adultId, "M")),
            "10.1.0.7"
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error", containsString("more than once")))
    }

    @Test
    fun `POST contribute rejects a member who has already been paid for`() {
        val fx = createMemberRsvp("GiveAgain")
        // The page hides people who are already covered; this proves hiding is not the only control.
        val rsvp = rsvpRepository.findById(fx.rsvpId).get()
        val paid = Payment(
            rsvp = rsvp,
            amount = BigDecimal("100.00"),
            stripeSessionId = "sess_already_${fx.rsvpId}",
            status = PaymentStatus.COMPLETED
        )
        paid.lineItems.add(
            PaymentLineItem(
                payment = paid, familyMemberId = fx.adultId, familyMemberName = "GiveAgain Adult",
                ageGroup = AgeGroup.ADULT, amount = BigDecimal("100.00"),
                tshirtSize = TshirtSize.L, kind = LineItemKind.FEE
            )
        )
        paymentRepository.save(paid)

        postContribute(
            contributeJson(fx.rsvpId, 1500, 0, attendeeJson(fx.adultId, "L")),
            "10.1.0.8"
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error", containsString("already been paid for")))
    }

    @Test
    fun `POST contribute rejects a shirt with no size`() {
        val fx = createMemberRsvp("GiveNoSize")
        postContribute(
            contributeJson(fx.rsvpId, 1500, 0, """{"memberId":${fx.adultId},"wantsShirt":true}"""),
            "10.1.0.9"
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error", containsString("size is required")))
    }

    @Test
    fun `POST contribute rejects a shirt with an unknown size`() {
        val fx = createMemberRsvp("GiveBadSize")
        postContribute(
            contributeJson(fx.rsvpId, 1500, 0, attendeeJson(fx.adultId, "HUGE")),
            "10.1.0.10"
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error", containsString("Unknown T-shirt size 'HUGE'")))
    }

    @Test
    fun `POST contribute rejects a gift above the cap`() {
        val fx = createMemberRsvp("GiveTooMuch")
        postContribute(
            contributeJson(fx.rsvpId, 2_000_000, 2_000_000, attendeeJson(fx.adultId)),
            "10.1.0.11"
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error", containsString("capped")))
    }

    @Test
    fun `POST contribute rejects a total below the one dollar minimum`() {
        val fx = createMemberRsvp("GiveTooLittle")
        // amount is 100 so the DTO's @Min passes; the service floor is what has to reject this.
        postContribute(
            contributeJson(fx.rsvpId, 100, 50, attendeeJson(fx.adultId)),
            "10.1.0.12"
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error", containsString("Minimum donation is $1")))
    }

    @Test
    fun `POST contribute rejects a negative gift`() {
        val fx = createMemberRsvp("GiveNegative")
        postContribute(
            contributeJson(fx.rsvpId, 1000, -500, attendeeJson(fx.adultId, "L")),
            "10.1.0.13"
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error", containsString("cannot be negative")))
    }

    // --- What a completed donation checkout looks like afterwards ---

    private data class ContributionFixture(
        val payment: Payment,
        val shirt: PaymentLineItem,
        val noShirt: PaymentLineItem,
        val donation: PaymentLineItem
    )

    /** A COMPLETED donation checkout: one member with a $15 shirt, one admitted for $0, plus a $40 gift. */
    private fun createContributionPayment(fx: MemberRsvp): ContributionFixture {
        val rsvp = rsvpRepository.findById(fx.rsvpId).get()
        val payment = Payment(
            rsvp = rsvp,
            amount = BigDecimal("55.00"),
            stripeSessionId = "sess_contrib_${fx.rsvpId}",
            status = PaymentStatus.COMPLETED
        )
        val shirt = PaymentLineItem(
            payment = payment, familyMemberId = fx.adultId, familyMemberName = "Shirt Adult",
            ageGroup = AgeGroup.ADULT, amount = BigDecimal("15.00"),
            tshirtSize = TshirtSize.L, kind = LineItemKind.SHIRT
        )
        val noShirt = PaymentLineItem(
            payment = payment, familyMemberId = fx.childId, familyMemberName = "NoShirt Child",
            ageGroup = AgeGroup.CHILD, amount = BigDecimal.ZERO, kind = LineItemKind.ATTENDEE
        )
        val donation = PaymentLineItem(
            payment = payment, guestName = "Donation",
            ageGroup = AgeGroup.ADULT, amount = BigDecimal("40.00"), kind = LineItemKind.DONATION
        )
        payment.lineItems.addAll(listOf(shirt, noShirt, donation))
        paymentRepository.save(payment)
        return ContributionFixture(payment, shirt, noShirt, donation)
    }

    @Test
    fun `GET summary counts donation and shirt money toward the branch balance`() {
        val fx = createMemberRsvp("GiveSummary")
        createContributionPayment(fx)

        mockMvc.perform(get("/api/payments/summary/${fx.rsvpId}"))
            .andExpect(status().isOk)
            // $100 adult + $50 child owed; the whole $55 counts, unlike an Angel gift.
            .andExpect(jsonPath("$.totalOwed").value(150.00))
            .andExpect(jsonPath("$.totalPaid").value(55.00))
            .andExpect(jsonPath("$.balance").value(95.00))
            .andExpect(jsonPath("$.status").value("PARTIAL"))
    }

    @Test
    fun `GET summary lists both donated-for members as paid but never the donation row`() {
        val fx = createMemberRsvp("GivePaidList")
        val c = createContributionPayment(fx)

        mockMvc.perform(get("/api/payments/summary/${fx.rsvpId}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.paidMemberIds", hasSize<Any>(2)))
            .andExpect(jsonPath("$.paidMemberIds", containsInAnyOrder(fx.adultId.toInt(), fx.childId.toInt())))
            .andExpect(jsonPath("$.paidMembers[?(@.lineItemId == ${c.shirt.id})].tshirtSize").value("L"))
            // The $40 gift row is money, not a guest — it must not surface as a person.
            .andExpect(jsonPath("$.paidGuests", hasSize<Any>(0)))
    }

    @Test
    fun `PUT line item size refuses an attendee who never bought a shirt`() {
        val fx = createMemberRsvp("GiveFreeShirt")
        val c = createContributionPayment(fx)

        // Without this, selecting everyone with no shirts and giving $1 would be a route to free
        // shirts: the ticket and pay pages both let a paid attendee set their own size.
        mockMvc.perform(
            put("/api/payments/line-items/${c.noShirt.id}/size")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"rsvpId":${fx.rsvpId},"tshirtSize":"M"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error", containsString("does not have a T-shirt")))

        // The shirt buyer on the same payment can still change theirs.
        mockMvc.perform(
            put("/api/payments/line-items/${c.shirt.id}/size")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"rsvpId":${fx.rsvpId},"tshirtSize":"XL"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.tshirtSize").value("XL"))
    }

    @Test
    fun `PUT line item size refuses the donation row itself`() {
        val fx = createMemberRsvp("GiveDonationRow")
        val c = createContributionPayment(fx)

        mockMvc.perform(
            put("/api/payments/line-items/${c.donation.id}/size")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"rsvpId":${fx.rsvpId},"tshirtSize":"M"}""")
        )
            .andExpect(status().isBadRequest)
    }

    // --- Revenue breakdown ---

    @Test
    fun `GET revenue splits completed income by what it paid for`() {
        val feeRsvpId = createRsvp("RevenueFees", adults = 1, children = 1)
        createSizedPayment(feeRsvpId)                  // $100 + $50 fees, $25 angel
        val fx = createMemberRsvp("RevenueGifts")
        createContributionPayment(fx)                  // $15 shirt, $0 attendee, $40 donation
        createStandaloneGift("revenue", amount = "10.00")

        mockMvc.perform(get("/api/payments/revenue"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.fees").value(150.00))
            .andExpect(jsonPath("$.shirts").value(15.00))
            .andExpect(jsonPath("$.donations").value(40.00))
            .andExpect(jsonPath("$.angel").value(35.00))
            .andExpect(jsonPath("$.total").value(240.00))
    }

    @Test
    fun `GET revenue ignores payments that never completed`() {
        val rsvpId = createRsvp("RevenuePending", adults = 1, children = 1)
        createSizedPayment(rsvpId, status = PaymentStatus.PENDING)

        mockMvc.perform(get("/api/payments/revenue"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(0))
    }

    // --- Guests on a donation checkout ---

    @Test
    fun `POST contribute accepts a guest with a shirt`() {
        val fx = createMemberRsvp("GiveGuest")
        postContribute(
            contributeJson(fx.rsvpId, 3000, 1500, guestJson("Cousin Ray", "ADULT", "XL")),
            "10.2.0.1"
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error", containsString("Stripe")))
    }

    @Test
    fun `POST contribute accepts a guest with no shirt`() {
        val fx = createMemberRsvp("GiveGuestBare")
        postContribute(
            contributeJson(fx.rsvpId, 2000, 2000, guestJson("Plus One")),
            "10.2.0.2"
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error", containsString("Stripe")))
    }

    @Test
    fun `POST contribute prices guests and members alike`() {
        val fx = createMemberRsvp("GiveMixed")
        // One member shirt + one guest shirt + $10 gift = $40. A guest shirt costs the same $15 as a
        // member's — a guest is not charged their age-group fee on this page.
        postContribute(
            contributeJson(
                fx.rsvpId, 4000, 1000,
                attendeeJson(fx.adultId, "L"),
                guestJson("Cousin Ray", "CHILD", "YM")
            ),
            "10.2.0.3"
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error", containsString("Stripe")))

        // Same people, a total that assumes an age-group fee for the guest instead of $15.
        postContribute(
            contributeJson(
                fx.rsvpId, 6500, 1000,
                attendeeJson(fx.adultId, "L"),
                guestJson("Cousin Ray", "CHILD", "YM")
            ),
            "10.2.0.4"
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error", containsString("Amount mismatch")))
    }

    @Test
    fun `POST contribute rejects a guest with a blank name`() {
        val fx = createMemberRsvp("GiveBlankGuest")
        postContribute(
            contributeJson(fx.rsvpId, 1500, 0, guestJson("   ", "ADULT", "L")),
            "10.2.0.5"
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error", containsString("needs a name")))
    }

    @Test
    fun `POST contribute rejects a guest named after the angel or donation sentinel`() {
        val fx = createMemberRsvp("GiveReserved")
        // isAngel still recognises a pre-V9 row by this name, so such a guest would be counted as a
        // gift and dropped from the ticket they just paid for.
        postContribute(
            contributeJson(fx.rsvpId, 1500, 0, guestJson("Angel Contribution", "ADULT", "L")),
            "10.2.0.6"
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error", containsString("reserved name")))

        postContribute(
            contributeJson(fx.rsvpId, 1500, 0, guestJson("donation", "ADULT", "L")),
            "10.2.0.7"
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error", containsString("reserved name")))
    }

    @Test
    fun `POST contribute rejects an attendee that is neither a member nor a guest`() {
        val fx = createMemberRsvp("GiveNeither")
        postContribute(
            contributeJson(fx.rsvpId, 1500, 0, """{"wantsShirt":true,"tshirtSize":"L"}"""),
            "10.2.0.8"
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error", containsString("either a family member or a named guest")))
    }

    @Test
    fun `POST contribute rejects an attendee that is both a member and a guest`() {
        val fx = createMemberRsvp("GiveBoth2")
        postContribute(
            contributeJson(
                fx.rsvpId, 1500, 0,
                """{"memberId":${fx.adultId},"guestName":"Ray","wantsShirt":true,"tshirtSize":"L"}"""
            ),
            "10.2.0.9"
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error", containsString("either a family member or a named guest")))
    }

    @Test
    fun `POST contribute rejects a guest shirt with no size`() {
        val fx = createMemberRsvp("GiveGuestNoSize")
        postContribute(
            contributeJson(fx.rsvpId, 1500, 0, """{"guestName":"Ray","ageGroup":"ADULT","wantsShirt":true}"""),
            "10.2.0.10"
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error", containsString("size is required for Ray")))
    }

    @Test
    fun `POST contribute allows a guest for a family that has already paid in full`() {
        val fx = createMemberRsvp("GivePaidFamily")
        // Both members covered by a completed payment: the branch owes nothing, but a guest they are
        // bringing still needs a shirt. The already-covered guard must not block that.
        val rsvp = rsvpRepository.findById(fx.rsvpId).get()
        val paid = Payment(
            rsvp = rsvp,
            amount = BigDecimal("150.00"),
            stripeSessionId = "sess_full_${fx.rsvpId}",
            status = PaymentStatus.COMPLETED
        )
        paid.lineItems.add(
            PaymentLineItem(
                payment = paid, familyMemberId = fx.adultId, familyMemberName = "Paid Adult",
                ageGroup = AgeGroup.ADULT, amount = BigDecimal("100.00"),
                tshirtSize = TshirtSize.L, kind = LineItemKind.FEE
            )
        )
        paid.lineItems.add(
            PaymentLineItem(
                payment = paid, familyMemberId = fx.childId, familyMemberName = "Paid Child",
                ageGroup = AgeGroup.CHILD, amount = BigDecimal("50.00"),
                tshirtSize = TshirtSize.YM, kind = LineItemKind.FEE
            )
        )
        paymentRepository.save(paid)

        postContribute(
            contributeJson(fx.rsvpId, 1500, 0, guestJson("Cousin Ray", "ADULT", "XL")),
            "10.2.0.11"
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error", containsString("Stripe")))
    }

    @Test
    fun `GET summary reports a donated guest as a paid guest`() {
        val fx = createMemberRsvp("GiveGuestSummary")
        val rsvp = rsvpRepository.findById(fx.rsvpId).get()
        val payment = Payment(
            rsvp = rsvp,
            amount = BigDecimal("15.00"),
            stripeSessionId = "sess_guest_${fx.rsvpId}",
            status = PaymentStatus.COMPLETED
        )
        payment.lineItems.add(
            PaymentLineItem(
                payment = payment, guestName = "Cousin Ray", ageGroup = AgeGroup.ADULT,
                amount = BigDecimal("15.00"), tshirtSize = TshirtSize.XL, kind = LineItemKind.SHIRT
            )
        )
        paymentRepository.save(payment)

        mockMvc.perform(get("/api/payments/summary/${fx.rsvpId}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.paidGuests", hasSize<Any>(1)))
            .andExpect(jsonPath("$.paidGuests[0].name").value("Cousin Ray"))
            .andExpect(jsonPath("$.paidGuests[0].tshirtSize").value("XL"))
            // A guest is nobody's family member, so they must not appear as one.
            .andExpect(jsonPath("$.paidMemberIds", hasSize<Any>(0)))
    }
}
