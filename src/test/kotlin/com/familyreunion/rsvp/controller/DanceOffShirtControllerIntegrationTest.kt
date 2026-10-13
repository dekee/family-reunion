package com.familyreunion.rsvp.controller

import com.familyreunion.rsvp.config.FeatureConfig
import com.familyreunion.rsvp.model.AgeGroup
import com.familyreunion.rsvp.model.LineItemKind
import com.familyreunion.rsvp.model.Payment
import com.familyreunion.rsvp.model.PaymentLineItem
import com.familyreunion.rsvp.model.PaymentStatus
import com.familyreunion.rsvp.model.TshirtSize
import com.familyreunion.rsvp.repository.PaymentRepository
import java.math.BigDecimal
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * The Sister Dance Off shirt page: a public, RSVP-free order priced server-side. Stripe is
 * unconfigured under the test profile, so a valid order ends at a 400 naming Stripe — which proves
 * it passed every rule before it.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = ["ADMIN"])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class DanceOffShirtControllerIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val paymentRepository: PaymentRepository,
    private val featureConfig: FeatureConfig
) {

    private fun order(json: String, ip: String = "10.9.0.1") = mockMvc.perform(
        post("/api/payments/dance-off-shirts")
            .header("X-Forwarded-For", ip)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json)
    )

    private fun item(size: String?, quantity: Int = 1, wearer: String? = null): String {
        val sizeJson = size?.let { "\"$it\"" } ?: "null"
        val wearerJson = wearer?.let { "\"$it\"" } ?: "null"
        return """{"tshirtSize":$sizeJson,"quantity":$quantity,"wearerName":$wearerJson}"""
    }

    private fun orderJson(totalCents: Long, vararg items: String, buyer: String? = "Kendra Tumblin"): String {
        val buyerJson = buyer?.let { "\"$it\"" } ?: "null"
        return """{"buyerName":$buyerJson,"totalCents":$totalCents,"items":[${items.joinToString(",")}]}"""
    }

    // --- Validation ---

    @Test
    fun `GET fees reports the dance-off price and whether orders are open`() {
        mockMvc.perform(get("/api/payments/fees"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.DANCE_OFF_SHIRT").value(2650))
            .andExpect(jsonPath("$.DANCE_OFF_SHIRTS_ENABLED").value(true))

        featureConfig.danceOffShirts = false
        mockMvc.perform(get("/api/payments/fees"))
            .andExpect(jsonPath("$.DANCE_OFF_SHIRTS_ENABLED").value(false))
    }

    @Test
    fun `a valid order passes validation and reaches Stripe`() {
        // Two L for the buyer and one YM for a named child: 3 x $26.50.
        order(orderJson(7950, item("L", 2), item("YM", 1, "Little Myra")))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error", containsString("Stripe")))
    }

    @Test
    fun `orders are refused while the switch is off`() {
        featureConfig.danceOffShirts = false
        order(orderJson(2650, item("M")))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error", containsString("closed")))
    }

    @Test
    fun `an order needs a buyer name`() {
        order(orderJson(2650, item("M"), buyer = null))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error", containsString("your name")))
        order(orderJson(2650, item("M"), buyer = "   "))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error", containsString("your name")))
    }

    @Test
    fun `an order needs at least one shirt`() {
        order(orderJson(0))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error", containsString("at least one shirt")))
    }

    @Test
    fun `an unknown or missing size is refused`() {
        order(orderJson(2650, item("XXXXXL")))
            .andExpect(status().isBadRequest)
        order(orderJson(2650, item(null)))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `a line quantity outside 1 to 10 is refused`() {
        order(orderJson(0, item("L", 0)))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error", containsString("1 to 10")))
        order(orderJson(11 * 2650, item("L", 11)))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error", containsString("1 to 10")))
    }

    @Test
    fun `more than 20 shirts in one order is refused`() {
        order(orderJson(21 * 2650, item("L", 10), item("M", 10), item("S", 1)))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error", containsString("capped at 20")))
    }

    @Test
    fun `a total that does not match the server price is refused`() {
        // The reunion shirt price, not the dance-off one.
        order(orderJson(1500, item("L")))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error", containsString("Amount mismatch")))
    }

    @Test
    fun `reserved line-item names are refused for the buyer and the wearer`() {
        order(orderJson(2650, item("L"), buyer = "Angel Contribution"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error", containsString("reserved")))
        order(orderJson(2650, item("L", 1, "donation")))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error", containsString("reserved")))
    }

    // --- What a completed order looks like afterwards ---

    /** A COMPLETED order: an L for the buyer and a YM for a named child, $26.50 each. */
    private fun createCompletedOrder(status: PaymentStatus = PaymentStatus.COMPLETED): Payment {
        val payment = Payment(
            rsvp = null,
            amount = BigDecimal("53.00"),
            stripeSessionId = "sess_danceoff_${System.nanoTime()}",
            status = status,
            donorName = "Kendra Tumblin"
        )
        payment.lineItems.addAll(
            listOf(
                PaymentLineItem(
                    payment = payment, guestName = "Kendra Tumblin", ageGroup = AgeGroup.ADULT,
                    amount = BigDecimal("26.50"), tshirtSize = TshirtSize.L, kind = LineItemKind.DANCE_OFF_SHIRT
                ),
                PaymentLineItem(
                    payment = payment, guestName = "Little Myra", ageGroup = AgeGroup.ADULT,
                    amount = BigDecimal("26.50"), tshirtSize = TshirtSize.YM, kind = LineItemKind.DANCE_OFF_SHIRT
                )
            )
        )
        return paymentRepository.save(payment)
    }

    @Test
    fun `history labels an order as a dance-off order, not an Angel Fund gift`() {
        createCompletedOrder()

        mockMvc.perform(get("/api/payments/history"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].familyName").value("Sister Dance Off shirt order"))
            .andExpect(jsonPath("$[0].danceOffOrder").value(true))
            .andExpect(jsonPath("$[0].donationOnly").value(false))
            .andExpect(jsonPath("$[0].donorName").value("Kendra Tumblin"))
            .andExpect(jsonPath("$[0].lineItems[0].kind").value("DANCE_OFF_SHIRT"))
            .andExpect(jsonPath("$[0].lineItems[0].tshirtSize").value("L"))
    }

    @Test
    fun `revenue reports dance-off shirts in their own bucket, apart from reunion shirts`() {
        createCompletedOrder()
        createCompletedOrder(status = PaymentStatus.PENDING)

        mockMvc.perform(get("/api/payments/revenue"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.danceOffShirts").value(53.00))
            .andExpect(jsonPath("$.shirts").value(0))
            .andExpect(jsonPath("$.total").value(53.00))
    }

    @Test
    fun `an order is nobody's ticket, so check-in neither counts nor admits it`() {
        val payment = createCompletedOrder()

        mockMvc.perform(get("/api/checkin/status"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(0))

        mockMvc.perform(post("/api/checkin/${payment.checkinToken}"))
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message", containsString("no attendees")))
    }
}
