package com.familyreunion.rsvp.repository

import com.familyreunion.rsvp.model.Payment
import org.springframework.data.jpa.repository.JpaRepository

interface PaymentRepository : JpaRepository<Payment, Long> {
    fun findByRsvpId(rsvpId: Long): List<Payment>
    fun findByStripeSessionId(stripeSessionId: String): Payment?
}
