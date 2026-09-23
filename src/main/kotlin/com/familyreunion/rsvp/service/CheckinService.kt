package com.familyreunion.rsvp.service

import com.familyreunion.rsvp.dto.*
import com.familyreunion.rsvp.model.PaymentStatus
import com.familyreunion.rsvp.model.TshirtSize
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

        // A standalone Angel Fund gift has a token but nobody to admit. Report it as an invalid
        // token (404) rather than "is a donation", so the endpoint can't be used to discover
        // which tokens belong to gifts.
        if (isDonationOnly(payment, paymentLineItemRepository.findByPaymentId(payment.id))) {
            throw IllegalArgumentException("Invalid ticket token")
        }

        return toTicketResponse(payment)
    }

    fun checkin(token: String): CheckinResponse {
        val payment = paymentRepository.findByCheckinToken(token)
            ?: return CheckinResponse(false, "Invalid ticket token")

        if (payment.status != PaymentStatus.COMPLETED) {
            return CheckinResponse(false, "Payment not completed")
        }

        if (isDonationOnly(payment, paymentLineItemRepository.findByPaymentId(payment.id))) {
            return CheckinResponse(false, "This payment is an Angel Fund gift — no attendees to check in")
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
        // Standalone Angel Fund gifts are completed payments with no attendees — they must not
        // inflate the expected-ticket count. Both counters come off the same filtered list so
        // total and checkedIn cannot drift.
        val ticketPayments = paymentRepository.findAll()
            .filter { it.status == PaymentStatus.COMPLETED && it.rsvp != null }

        val tickets = ticketPayments.map { toTicketResponse(it) }

        return CheckinStatusResponse(
            total = tickets.size,
            checkedIn = ticketPayments.count { it.checkedIn },
            tickets = tickets
        )
    }

    /** Token-scoped edit of T-shirt sizes for the attendees on a completed ticket. */
    fun updateTicketSizes(token: String, request: UpdateTicketSizesRequest): TicketResponse {
        val payment = paymentRepository.findByCheckinToken(token)
            ?: throw IllegalArgumentException("Invalid ticket token")

        if (payment.status != PaymentStatus.COMPLETED) {
            throw IllegalArgumentException("Payment not completed")
        }

        val lineItemsById = paymentLineItemRepository.findByPaymentId(payment.id).associateBy { it.id }
        val changed = request.sizes.map { entry ->
            val lineItem = lineItemsById[entry.lineItemId]
                ?: throw IllegalArgumentException("Attendee ${entry.lineItemId} is not on this ticket")
            if (lineItem.isAngel) {
                throw IllegalArgumentException("Angel contributions do not have a T-shirt size")
            }
            lineItem.tshirtSize = TshirtSize.parse(entry.tshirtSize, lineItem.displayName)
            lineItem
        }
        paymentLineItemRepository.saveAll(changed)

        return toTicketResponse(payment)
    }

    /**
     * True when nothing on this payment is a person: a standalone Angel Fund gift (no RSVP), or a
     * payment whose only line item is the angel pseudo-row. Takes the line items as a parameter
     * rather than reading [Payment.lineItems], which is LAZY and would break a non-transactional
     * caller.
     */
    private fun isDonationOnly(
        payment: com.familyreunion.rsvp.model.Payment,
        lineItems: List<com.familyreunion.rsvp.model.PaymentLineItem>
    ): Boolean = payment.rsvp == null || lineItems.none { !it.isAngel }

    private fun toTicketResponse(payment: com.familyreunion.rsvp.model.Payment): TicketResponse {
        val lineItems = paymentLineItemRepository.findByPaymentId(payment.id)
        // Angel contributions are donations, not people — never list them as ticket attendees.
        val attendees = lineItems.filter { !it.isAngel }.map { li ->
            TicketAttendee(
                name = li.displayName,
                ageGroup = li.ageGroup.name,
                isGuest = li.guestName != null,
                lineItemId = li.id,
                tshirtSize = li.tshirtSize?.name
            )
        }

        return TicketResponse(
            checkinToken = payment.checkinToken,
            familyName = payment.rsvp?.familyName ?: "",
            payerName = payment.payerName ?: payment.rsvp?.headOfHouseholdName ?: "",
            amount = payment.amount,
            checkedIn = payment.checkedIn,
            checkedInAt = payment.checkedInAt?.toString(),
            attendees = attendees
        )
    }
}
