package com.familyreunion.rsvp.service

import com.familyreunion.rsvp.dto.AttendeeDto
import com.familyreunion.rsvp.dto.RsvpRequest
import com.familyreunion.rsvp.dto.RsvpResponse
import com.familyreunion.rsvp.dto.RsvpSummaryResponse
import com.familyreunion.rsvp.exception.FamilyMemberNotFoundException
import com.familyreunion.rsvp.exception.RsvpNotFoundException
import com.familyreunion.rsvp.model.AgeGroup
import com.familyreunion.rsvp.model.Attendee
import com.familyreunion.rsvp.model.Rsvp
import com.familyreunion.rsvp.repository.FamilyMemberRepository
import com.familyreunion.rsvp.repository.RsvpRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class RsvpService(
    private val rsvpRepository: RsvpRepository,
    private val familyMemberRepository: FamilyMemberRepository
) {

    fun createRsvp(request: RsvpRequest): RsvpResponse {
        val rsvp = Rsvp(
            familyName = request.familyName,
            headOfHouseholdName = request.headOfHouseholdName,
            email = request.email,
            phone = request.phone,
            needsLodging = request.needsLodging,
            arrivalDate = request.arrivalDate,
            departureDate = request.departureDate,
            notes = request.notes
        )
        addAttendees(rsvp, request.attendees)
        val saved = rsvpRepository.save(rsvp)
        return toResponse(saved)
    }

    @Transactional(readOnly = true)
    fun getAllRsvps(): List<RsvpResponse> {
        return rsvpRepository.findAll().map { toResponse(it) }
    }

    @Transactional(readOnly = true)
    fun getRsvpById(id: Long): RsvpResponse {
        val rsvp = rsvpRepository.findById(id)
            .orElseThrow { RsvpNotFoundException(id) }
        return toResponse(rsvp)
    }

    fun updateRsvp(id: Long, request: RsvpRequest): RsvpResponse {
        val existing = rsvpRepository.findById(id)
            .orElseThrow { RsvpNotFoundException(id) }

        existing.familyName = request.familyName
        existing.headOfHouseholdName = request.headOfHouseholdName
        existing.email = request.email
        existing.phone = request.phone
        existing.needsLodging = request.needsLodging
        existing.arrivalDate = request.arrivalDate
        existing.departureDate = request.departureDate
        existing.notes = request.notes

        existing.attendees.clear()
        addAttendees(existing, request.attendees)

        val saved = rsvpRepository.save(existing)
        return toResponse(saved)
    }

    fun deleteRsvp(id: Long) {
        if (!rsvpRepository.existsById(id)) {
            throw RsvpNotFoundException(id)
        }
        rsvpRepository.deleteById(id)
    }

    @Transactional(readOnly = true)
    fun getSummary(): RsvpSummaryResponse {
        val allRsvps = rsvpRepository.findAll()
        val allAttendees = allRsvps.flatMap { it.attendees }

        return RsvpSummaryResponse(
            totalFamilies = allRsvps.size,
            totalHeadcount = allAttendees.size,
            adultCount = allAttendees.count { it.ageGroup == AgeGroup.ADULT },
            childCount = allAttendees.count { it.ageGroup == AgeGroup.CHILD },
            infantCount = allAttendees.count { it.ageGroup == AgeGroup.INFANT },
            lodgingCount = allRsvps.count { it.needsLodging }
        )
    }

    private fun addAttendees(rsvp: Rsvp, attendeeDtos: List<AttendeeDto>) {
        attendeeDtos.forEach { dto ->
            val attendee = if (dto.familyMemberId != null) {
                val member = familyMemberRepository.findById(dto.familyMemberId)
                    .orElseThrow { FamilyMemberNotFoundException(dto.familyMemberId) }
                Attendee(
                    rsvp = rsvp,
                    familyMember = member,
                    dietaryNeeds = dto.dietaryNeeds
                )
            } else {
                Attendee(
                    rsvp = rsvp,
                    guestName = dto.guestName,
                    guestAgeGroup = dto.guestAgeGroup,
                    dietaryNeeds = dto.dietaryNeeds
                )
            }
            rsvp.attendees.add(attendee)
        }
    }

    private fun toResponse(rsvp: Rsvp) = RsvpResponse(
        id = rsvp.id,
        familyName = rsvp.familyName,
        headOfHouseholdName = rsvp.headOfHouseholdName,
        email = rsvp.email,
        phone = rsvp.phone,
        attendees = rsvp.attendees.map { toAttendeeDto(it) },
        needsLodging = rsvp.needsLodging,
        arrivalDate = rsvp.arrivalDate,
        departureDate = rsvp.departureDate,
        notes = rsvp.notes
    )

    private fun toAttendeeDto(attendee: Attendee) = AttendeeDto(
        id = attendee.id,
        familyMemberId = attendee.familyMember?.id,
        familyMemberName = attendee.familyMember?.name,
        familyMemberAgeGroup = attendee.familyMember?.ageGroup,
        familyMemberParentName = attendee.familyMember?.parent?.name,
        guestName = attendee.guestName,
        guestAgeGroup = attendee.guestAgeGroup,
        dietaryNeeds = attendee.dietaryNeeds
    )
}
