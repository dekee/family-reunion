package com.familyreunion.rsvp.service

import com.familyreunion.rsvp.dto.FamilyMemberDto
import com.familyreunion.rsvp.dto.RsvpRequest
import com.familyreunion.rsvp.dto.RsvpResponse
import com.familyreunion.rsvp.dto.RsvpSummaryResponse
import com.familyreunion.rsvp.exception.RsvpNotFoundException
import com.familyreunion.rsvp.model.AgeGroup
import com.familyreunion.rsvp.model.FamilyMember
import com.familyreunion.rsvp.model.Rsvp
import com.familyreunion.rsvp.repository.RsvpRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class RsvpService(private val rsvpRepository: RsvpRepository) {

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
        request.familyMembers.forEach { dto ->
            val member = FamilyMember(
                name = dto.name,
                ageGroup = dto.ageGroup,
                dietaryNeeds = dto.dietaryNeeds,
                rsvp = rsvp
            )
            rsvp.familyMembers.add(member)
        }
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

        existing.familyMembers.clear()
        request.familyMembers.forEach { dto ->
            val member = FamilyMember(
                name = dto.name,
                ageGroup = dto.ageGroup,
                dietaryNeeds = dto.dietaryNeeds,
                rsvp = existing
            )
            existing.familyMembers.add(member)
        }

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
        val allMembers = allRsvps.flatMap { it.familyMembers }

        return RsvpSummaryResponse(
            totalFamilies = allRsvps.size,
            totalHeadcount = allMembers.size,
            adultCount = allMembers.count { it.ageGroup == AgeGroup.ADULT },
            childCount = allMembers.count { it.ageGroup == AgeGroup.CHILD },
            infantCount = allMembers.count { it.ageGroup == AgeGroup.INFANT },
            lodgingCount = allRsvps.count { it.needsLodging }
        )
    }

    private fun toResponse(rsvp: Rsvp) = RsvpResponse(
        id = rsvp.id,
        familyName = rsvp.familyName,
        headOfHouseholdName = rsvp.headOfHouseholdName,
        email = rsvp.email,
        phone = rsvp.phone,
        familyMembers = rsvp.familyMembers.map { toMemberDto(it) },
        needsLodging = rsvp.needsLodging,
        arrivalDate = rsvp.arrivalDate,
        departureDate = rsvp.departureDate,
        notes = rsvp.notes
    )

    private fun toMemberDto(member: FamilyMember) = FamilyMemberDto(
        id = member.id,
        name = member.name,
        ageGroup = member.ageGroup,
        dietaryNeeds = member.dietaryNeeds
    )
}
