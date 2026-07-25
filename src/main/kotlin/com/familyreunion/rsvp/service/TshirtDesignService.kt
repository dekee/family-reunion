package com.familyreunion.rsvp.service

import com.familyreunion.rsvp.dto.DesignResponse
import com.familyreunion.rsvp.dto.DesignVoteRequest
import com.familyreunion.rsvp.exception.DesignNotFoundException
import com.familyreunion.rsvp.exception.FamilyMemberNotFoundException
import com.familyreunion.rsvp.model.TshirtDesign
import com.familyreunion.rsvp.model.TshirtDesignVote
import com.familyreunion.rsvp.repository.FamilyMemberRepository
import com.familyreunion.rsvp.repository.TshirtDesignRepository
import com.familyreunion.rsvp.repository.TshirtDesignVoteRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class TshirtDesignService(
    private val designRepository: TshirtDesignRepository,
    private val voteRepository: TshirtDesignVoteRepository,
    private val familyMemberRepository: FamilyMemberRepository
) {

    @Transactional(readOnly = true)
    fun getAllDesigns(): List<DesignResponse> {
        return designRepository.findAllByOrderByIdAsc().map { toResponse(it) }
    }

    fun vote(request: DesignVoteRequest): DesignResponse {
        val design = designRepository.findById(request.designId)
            .orElseThrow { DesignNotFoundException(request.designId) }

        val member = familyMemberRepository.findById(request.familyMemberId)
            .orElseThrow { FamilyMemberNotFoundException(request.familyMemberId) }

        val existing = voteRepository.findByFamilyMember(member)

        if (existing != null) {
            existing.design = design
            existing.votedAt = LocalDateTime.now()
            voteRepository.save(existing)
        } else {
            val vote = TshirtDesignVote(
                design = design,
                familyMember = member,
                votedAt = LocalDateTime.now()
            )
            voteRepository.save(vote)
        }

        return toResponse(designRepository.findById(request.designId).get())
    }

    @Transactional(readOnly = true)
    fun getVoterChoice(familyMemberId: Long): Long? {
        val member = familyMemberRepository.findById(familyMemberId).orElse(null) ?: return null
        return voteRepository.findByFamilyMember(member)?.design?.id
    }

    private fun toResponse(design: TshirtDesign) = DesignResponse(
        id = design.id,
        name = design.name,
        imageUrl = design.imageUrl,
        voteCount = design.votes.size
    )
}
