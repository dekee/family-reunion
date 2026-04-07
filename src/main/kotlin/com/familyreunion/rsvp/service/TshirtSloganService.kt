package com.familyreunion.rsvp.service

import com.familyreunion.rsvp.dto.SloganRequest
import com.familyreunion.rsvp.dto.SloganResponse
import com.familyreunion.rsvp.dto.SloganVoteRequest
import com.familyreunion.rsvp.exception.SloganNotFoundException
import com.familyreunion.rsvp.model.TshirtSlogan
import com.familyreunion.rsvp.model.TshirtVote
import com.familyreunion.rsvp.repository.TshirtSloganRepository
import com.familyreunion.rsvp.repository.TshirtVoteRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class TshirtSloganService(
    private val sloganRepository: TshirtSloganRepository,
    private val voteRepository: TshirtVoteRepository
) {

    @Transactional(readOnly = true)
    fun getAllSlogans(): List<SloganResponse> {
        return sloganRepository.findAllByOrderByIdAsc().map { toResponse(it) }
    }

    fun vote(request: SloganVoteRequest): SloganResponse {
        val slogan = sloganRepository.findById(request.sloganId)
            .orElseThrow { SloganNotFoundException(request.sloganId) }

        val normalizedName = request.voterName.trim().lowercase()
        val existing = voteRepository.findByVoterName(normalizedName)

        if (existing != null) {
            // Change their vote to the new slogan
            existing.slogan = slogan
            existing.votedAt = LocalDateTime.now()
            voteRepository.save(existing)
        } else {
            val vote = TshirtVote(
                slogan = slogan,
                voterName = normalizedName,
                votedAt = LocalDateTime.now()
            )
            voteRepository.save(vote)
        }

        return toResponse(sloganRepository.findById(request.sloganId).get())
    }

    @Transactional(readOnly = true)
    fun getVoterChoice(voterName: String): Long? {
        val normalizedName = voterName.trim().lowercase()
        return voteRepository.findByVoterName(normalizedName)?.slogan?.id
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
