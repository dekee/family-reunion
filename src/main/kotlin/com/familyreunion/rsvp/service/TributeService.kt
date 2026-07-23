package com.familyreunion.rsvp.service

import com.familyreunion.rsvp.dto.TributeRequest
import com.familyreunion.rsvp.dto.TributeResponse
import com.familyreunion.rsvp.exception.FamilyMemberNotFoundException
import com.familyreunion.rsvp.exception.TributeNotFoundException
import com.familyreunion.rsvp.model.Tribute
import com.familyreunion.rsvp.repository.FamilyMemberRepository
import com.familyreunion.rsvp.repository.TributeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class TributeService(
    private val tributeRepository: TributeRepository,
    private val familyMemberRepository: FamilyMemberRepository
) {

    @Transactional(readOnly = true)
    fun getAllTributes(): List<TributeResponse> {
        return tributeRepository.findAllByOrderByCreatedAtDesc().map { toResponse(it) }
    }

    fun submitTribute(request: TributeRequest): TributeResponse {
        val sibling = familyMemberRepository.findById(request.siblingId)
            .orElseThrow { FamilyMemberNotFoundException(request.siblingId) }

        // Only the 11 pillars (children of the founders) can receive tributes.
        if (sibling.parent?.isFounder != true) {
            throw IllegalStateException("Tributes can only honor one of the family pillars")
        }

        val author = familyMemberRepository.findById(request.authorId)
            .orElseThrow { FamilyMemberNotFoundException(request.authorId) }

        val story = request.story.trim()
        val existing = tributeRepository.findBySiblingAndAuthor(sibling, author)

        val saved = if (existing != null) {
            existing.story = story
            existing.updatedAt = LocalDateTime.now()
            tributeRepository.save(existing)
        } else {
            tributeRepository.save(Tribute(sibling = sibling, author = author, story = story))
        }

        return toResponse(saved)
    }

    fun deleteTribute(id: Long) {
        if (!tributeRepository.existsById(id)) {
            throw TributeNotFoundException(id)
        }
        tributeRepository.deleteById(id)
    }

    private fun toResponse(tribute: Tribute) = TributeResponse(
        id = tribute.id,
        siblingId = tribute.sibling.id,
        siblingName = tribute.sibling.name,
        authorId = tribute.author.id,
        authorName = tribute.author.name,
        story = tribute.story,
        createdAt = tribute.createdAt,
        updatedAt = tribute.updatedAt
    )
}
