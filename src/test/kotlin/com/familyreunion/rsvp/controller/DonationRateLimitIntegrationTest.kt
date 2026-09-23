package com.familyreunion.rsvp.controller

import org.hamcrest.Matchers.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

/**
 * Lives in its own class on purpose: IpRateLimiter state is per-process and survives
 * @DirtiesContext, so hammering /donate from a shared test class would make its neighbours flaky.
 * The dedicated X-Forwarded-For value keeps this test's bucket to itself.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = ["ADMIN"])
class DonationRateLimitIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc
) {

    @Test
    fun `POST donate rate-limits a client after five attempts`() {
        val ip = "203.0.113.77"
        val body = """{"amountCents":2500,"donorName":"Rate Limit","anonymous":false}"""

        repeat(5) {
            mockMvc.perform(
                post("/api/payments/donate")
                    .header("X-Forwarded-For", ip)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
            ).andExpect(status().isBadRequest)   // Stripe unconfigured: allowed through, then fails
        }

        mockMvc.perform(
            post("/api/payments/donate")
                .header("X-Forwarded-For", ip)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isTooManyRequests)
            .andExpect(jsonPath("$.error", containsString("Too many donation attempts")))
    }
}
