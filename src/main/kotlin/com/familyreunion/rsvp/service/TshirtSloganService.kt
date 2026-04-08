package com.familyreunion.rsvp.service

import com.familyreunion.rsvp.dto.SloganRequest
import com.familyreunion.rsvp.dto.SloganResponse
import com.familyreunion.rsvp.dto.SloganVoteRequest
import com.familyreunion.rsvp.exception.FamilyMemberNotFoundException
import com.familyreunion.rsvp.exception.SloganNotFoundException
import com.familyreunion.rsvp.model.TshirtSlogan
import com.familyreunion.rsvp.model.TshirtVote
import com.familyreunion.rsvp.repository.FamilyMemberRepository
import com.familyreunion.rsvp.repository.TshirtSloganRepository
import com.familyreunion.rsvp.repository.TshirtVoteRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class TshirtSloganService(
    private val sloganRepository: TshirtSloganRepository,
    private val voteRepository: TshirtVoteRepository,
    private val familyMemberRepository: FamilyMemberRepository
) {

    @Transactional(readOnly = true)
    fun getAllSlogans(): List<SloganResponse> {
        return sloganRepository.findAllByOrderByIdAsc().map { toResponse(it) }
    }

    fun vote(request: SloganVoteRequest): SloganResponse {
        val slogan = sloganRepository.findById(request.sloganId)
            .orElseThrow { SloganNotFoundException(request.sloganId) }

        val member = familyMemberRepository.findById(request.familyMemberId)
            .orElseThrow { FamilyMemberNotFoundException(request.familyMemberId) }

        val existing = voteRepository.findByFamilyMember(member)

        if (existing != null) {
            existing.slogan = slogan
            existing.votedAt = LocalDateTime.now()
            voteRepository.save(existing)
        } else {
            val vote = TshirtVote(
                slogan = slogan,
                familyMember = member,
                votedAt = LocalDateTime.now()
            )
            voteRepository.save(vote)
        }

        return toResponse(sloganRepository.findById(request.sloganId).get())
    }

    @Transactional(readOnly = true)
    fun getVoterChoice(familyMemberId: Long): Long? {
        val member = familyMemberRepository.findById(familyMemberId).orElse(null) ?: return null
        return voteRepository.findByFamilyMember(member)?.slogan?.id
    }

    fun createSlogan(request: SloganRequest): SloganResponse {
        val slogan = TshirtSlogan(slogan = request.slogan.trim())
        val saved = sloganRepository.save(slogan)
        return toResponse(saved)
    }

    fun deleteSlogan(id: Long) {
        if (!sloganRepository.existsById(id)) {
            throw SloganNotFoundException(id)
        }
        sloganRepository.deleteById(id)
    }

    private fun toResponse(slogan: TshirtSlogan) = SloganResponse(
        id = slogan.id,
        slogan = slogan.slogan,
        voteCount = slogan.votes.size
    )
}
