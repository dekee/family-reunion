package com.familyreunion.rsvp.controller

import com.familyreunion.rsvp.model.AgeGroup
import com.familyreunion.rsvp.model.Payment
import com.familyreunion.rsvp.model.PaymentLineItem
import com.familyreunion.rsvp.model.PaymentStatus
import com.familyreunion.rsvp.model.Rsvp
import com.familyreunion.rsvp.model.TshirtSize
import com.familyreunion.rsvp.repository.PaymentLineItemRepository
import com.familyreunion.rsvp.repository.PaymentRepository
import com.familyreunion.rsvp.repository.RsvpRepository
import java.math.BigDecimal
import org.assertj.core.api.Assertions.assertThat
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
class CheckinControllerIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val rsvpRepository: RsvpRepository,
    private val paymentRepository: PaymentRepository,
    private val paymentLineItemRepository: PaymentLineItemRepository
) {

    private data class Fixture(val payment: Payment, val adult: PaymentLineItem, val child: PaymentLineItem, val angel: PaymentLineItem)

    private fun createPayment(status: PaymentStatus = PaymentStatus.COMPLETED, familyName: String = "Ticket"): Fixture {
        val rsvp = rsvpRepository.save(Rsvp(
            familyName = familyName,
            headOfHouseholdName = "$familyName Head",
            email = "${familyName.lowercase()}@example.com"
        ))
        val payment = Payment(
            rsvp = rsvp,
            amount = BigDecimal("175.00"),
            stripeSessionId = "sess_${familyName.lowercase()}",
            status = status
        )
        val adult = PaymentLineItem(payment = payment, familyMemberId = 42, familyMemberName = "$familyName Adult",
            ageGroup = AgeGroup.ADULT, amount = BigDecimal("100.00"), tshirtSize = TshirtSize.L)
        val child = PaymentLineItem(payment = payment, guestName = "$familyName Kid",
            ageGroup = AgeGroup.CHILD, amount = BigDecimal("50.00"))
        val angel = PaymentLineItem(payment = payment, guestName = "Angel Contribution",
            ageGroup = AgeGroup.ADULT, amount = BigDecimal("25.00"))
        payment.lineItems.addAll(listOf(adult, child, angel))
        paymentRepository.save(payment)
        return Fixture(payment, adult, child, angel)
    }

    @Test
    fun `GET ticket returns attendees with lineItemId and tshirtSize`() {
        val fx = createPayment()

        mockMvc.perform(get("/api/checkin/ticket/${fx.payment.checkinToken}"))
            .andExpect(status().isOk)
            // Two, not three: the angel contribution is a donation, not a person on the ticket.
            .andExpect(jsonPath("$.attendees", hasSize<Any>(2)))
            .andExpect(jsonPath("$.attendees[*].name").value(not(hasItem("Angel Contribution"))))
            .andExpect(jsonPath("$.attendees[0].lineItemId").value(fx.adult.id))
            .andExpect(jsonPath("$.attendees[0].tshirtSize").value("L"))
            .andExpect(jsonPath("$.attendees[1].lineItemId").value(fx.child.id))
            .andExpect(jsonPath("$.attendees[1].tshirtSize").value(nullValue()))
    }

    @Test
    fun `PUT ticket sizes saves sizes and returns the updated ticket`() {
        val fx = createPayment()
        val json = """{"sizes":[{"lineItemId":${fx.child.id},"tshirtSize":"YM"},{"lineItemId":${fx.adult.id},"tshirtSize":"XL"}]}"""

        mockMvc.perform(
            put("/api/checkin/ticket/${fx.payment.checkinToken}/sizes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.checkinToken").value(fx.payment.checkinToken))
            .andExpect(jsonPath("$.attendees[0].tshirtSize").value("XL"))
            .andExpect(jsonPath("$.attendees[1].tshirtSize").value("YM"))

        assertThat(paymentLineItemRepository.findById(fx.child.id).get().tshirtSize).isEqualTo(TshirtSize.YM)
        assertThat(paymentLineItemRepository.findById(fx.adult.id).get().tshirtSize).isEqualTo(TshirtSize.XL)
    }

    @Test
    fun `PUT ticket sizes allows an adult size for a child`() {
        val fx = createPayment()
        val json = """{"sizes":[{"lineItemId":${fx.child.id},"tshirtSize":"L"}]}"""

        mockMvc.perform(
            put("/api/checkin/ticket/${fx.payment.checkinToken}/sizes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.attendees[1].tshirtSize").value("L"))

        assertThat(paymentLineItemRepository.findById(fx.child.id).get().tshirtSize).isEqualTo(TshirtSize.L)
    }

    @Test
    fun `PUT ticket sizes rejects an unknown size`() {
        val fx = createPayment()
        val json = """{"sizes":[{"lineItemId":${fx.child.id},"tshirtSize":"HUGE"}]}"""

        mockMvc.perform(
            put("/api/checkin/ticket/${fx.payment.checkinToken}/sizes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error", containsString("Unknown T-shirt size")))

        assertThat(paymentLineItemRepository.findById(fx.child.id).get().tshirtSize).isNull()
    }

    @Test
    fun `PUT ticket sizes rejects the angel contribution line item`() {
        val fx = createPayment()
        val json = """{"sizes":[{"lineItemId":${fx.angel.id},"tshirtSize":"L"}]}"""

        mockMvc.perform(
            put("/api/checkin/ticket/${fx.payment.checkinToken}/sizes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `PUT ticket sizes rejects a line item from another payment`() {
        val mine = createPayment(familyName = "Mine")
        val theirs = createPayment(familyName = "Theirs")
        val json = """{"sizes":[{"lineItemId":${theirs.child.id},"tshirtSize":"YS"}]}"""

        mockMvc.perform(
            put("/api/checkin/ticket/${mine.payment.checkinToken}/sizes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
        )
            .andExpect(status().isBadRequest)

        assertThat(paymentLineItemRepository.findById(theirs.child.id).get().tshirtSize).isNull()
    }

    @Test
    fun `PUT ticket sizes rejects a pending payment`() {
        val fx = createPayment(status = PaymentStatus.PENDING)
        val json = """{"sizes":[{"lineItemId":${fx.child.id},"tshirtSize":"YS"}]}"""

        mockMvc.perform(
            put("/api/checkin/ticket/${fx.payment.checkinToken}/sizes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `PUT ticket sizes rejects an unknown token`() {
        mockMvc.perform(
            put("/api/checkin/ticket/not-a-real-token/sizes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"sizes":[]}""")
        )
            .andExpect(status().isBadRequest)
    }

    // --- Standalone Angel Fund gifts are not tickets ---

    /** A COMPLETED gift with no RSVP and only an angel line item. */
    private fun createStandaloneGift(suffix: String): Payment {
        val payment = Payment(
            rsvp = null,
            amount = BigDecimal("50.00"),
            stripeSessionId = "sess_gift_$suffix",
            status = PaymentStatus.COMPLETED
        )
        payment.lineItems.add(PaymentLineItem(payment = payment, guestName = "Angel Contribution",
            ageGroup = AgeGroup.ADULT, amount = BigDecimal("50.00")))
        return paymentRepository.save(payment)
    }

    @Test
    fun `GET ticket treats a standalone gift token as invalid`() {
        val gift = createStandaloneGift("ticket")

        // 404, deliberately indistinguishable from a bad token, so the endpoint cannot be used to
        // discover which tokens belong to gifts.
        mockMvc.perform(get("/api/checkin/ticket/${gift.checkinToken}"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `POST checkin explains that a standalone gift has nobody to check in`() {
        val gift = createStandaloneGift("checkin")

        mockMvc.perform(post("/api/checkin/${gift.checkinToken}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message", containsString("Angel Fund gift")))
    }

    @Test
    fun `GET checkin status excludes standalone gifts from the expected ticket count`() {
        createPayment(familyName = "Real")
        createStandaloneGift("status")

        mockMvc.perform(get("/api/checkin/status"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.tickets", hasSize<Any>(1)))
            .andExpect(jsonPath("$.tickets[0].familyName").value("Real"))
    }
}
