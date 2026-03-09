package com.familyreunion.rsvp.service

import com.familyreunion.rsvp.dto.*
import com.familyreunion.rsvp.model.PaymentStatus
import com.familyreunion.rsvp.repository.PaymentLineItemRepository
import com.familyreunion.rsvp.repository.PaymentRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class CheckinService(
    private val paymentRepository: PaymentRepository,
    private val paymentLineItemRepository: PaymentLineItemRepository
) {

    @Transactional(readOnly = true)
    fun getTicket(token: String): TicketResponse {
        val payment = paymentRepository.findByCheckinToken(token)
            ?: throw IllegalArgumentException("Invalid ticket token")

        if (payment.status != PaymentStatus.COMPLETED) {
            throw IllegalArgumentException("Payment not completed")
        }

        return toTicketResponse(payment)
    }

    fun checkin(token: String): CheckinResponse {
        val payment = paymentRepository.findByCheckinToken(token)
            ?: return CheckinResponse(false, "Invalid ticket token")

        if (payment.status != PaymentStatus.COMPLETED) {
            return CheckinResponse(false, "Payment not completed")
        }

        if (payment.checkedIn) {
            return CheckinResponse(
                success = true,
                message = "Already checked in at ${payment.checkedInAt}",
                ticket = toTicketResponse(payment)
            )
        }

        payment.checkedIn = true
        payment.checkedInAt = LocalDateTime.now()
        paymentRepository.save(payment)

        return CheckinResponse(
            success = true,
            message = "Checked in successfully",
            ticket = toTicketResponse(payment)
        )
    }

    @Transactional(readOnly = true)
    fun getCheckinStatus(): CheckinStatusResponse {
        val completedPayments = paymentRepository.findAll()
            .filter { it.status == PaymentStatus.COMPLETED }

        val tickets = completedPayments.map { toTicketResponse(it) }

        return CheckinStatusResponse(
            total = tickets.size,
            checkedIn = completedPayments.count { it.checkedIn },
            tickets = tickets
        )
    }

    private fun toTicketResponse(payment: com.familyreunion.rsvp.model.Payment): TicketResponse {
        val lineItems = paymentLineItemRepository.findByPaymentId(payment.id)
        val attendees = lineItems.map { li ->
            TicketAttendee(
                name = li.familyMemberName ?: li.guestName ?: "Unknown",
                ageGroup = li.ageGroup.name,
                isGuest = li.guestName != null
            )
        }

        return TicketResponse(
            checkinToken = payment.checkinToken,
            familyName = payment.rsvp?.familyName ?: "",
            payerName = payment.payerName ?: payment.rsvp?.headOfHouseholdName ?: "",
            payerEmail = payment.payerEmail,
            amount = payment.amount,
            checkedIn = payment.checkedIn,
            checkedInAt = payment.checkedInAt?.toString(),
            attendees = attendees
        )
    }
}
