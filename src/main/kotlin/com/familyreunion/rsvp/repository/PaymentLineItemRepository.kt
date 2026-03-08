package com.familyreunion.rsvp.repository

import com.familyreunion.rsvp.model.PaymentLineItem
import com.familyreunion.rsvp.model.PaymentStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface PaymentLineItemRepository : JpaRepository<PaymentLineItem, Long> {
    fun findByPaymentId(paymentId: Long): List<PaymentLineItem>

    @Query("SELECT li FROM PaymentLineItem li WHERE li.payment.id IN :paymentIds AND li.payment.status = :status")
    fun findByCompletedPaymentIds(paymentIds: List<Long>, status: PaymentStatus): List<PaymentLineItem>
}
