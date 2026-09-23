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

        // A standalone gift's donor is never handed this token, so the token resolving at all
        // would only ever be someone probing. Reported as an invalid token so the endpoint can't
        // be used to discover which tokens belong to gifts.
        //
        // An *in-branch* angel-only payment is different: its payer WAS handed a ticket link on
        // the pay page, so it keeps resolving and the ticket page thanks them for the gift
        // instead of listing an empty party.
        if (payment.rsvp == null) {
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

        if (hasNoAttendees(paymentLineItemRepository.findByPaymentId(payment.id))) {
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
        // Angel Fund gifts are completed payments with nobody to admit — whether they are
        // standalone or were made alongside a branch checkout, they must not inflate the
        // expected-ticket count. Filtering on the built ticket's attendees covers both, and both
        // counters come off the same list so total and checkedIn cannot drift.
        val ticketed = paymentRepository.findAll()
            .filter { it.status == PaymentStatus.COMPLETED }
            .map { it to toTicketResponse(it) }
            .filter { (_, ticket) -> ticket.attendees.isNotEmpty() }

        return CheckinStatusResponse(
            total = ticketed.size,
            checkedIn = ticketed.count { (payment, _) -> payment.checkedIn },
            tickets = ticketed.map { it.second }
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
     * True when no line item on this payment is a person — i.e. the payment is nothing but an
     * Angel Fund gift. Takes the line items as a parameter rather than reading
     * [Payment.lineItems], which is LAZY and would break a non-transactional caller.
     */
    private fun hasNoAttendees(
        lineItems: List<com.familyreunion.rsvp.model.PaymentLineItem>
    ): Boolean = lineItems.none { !it.isAngel }

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
