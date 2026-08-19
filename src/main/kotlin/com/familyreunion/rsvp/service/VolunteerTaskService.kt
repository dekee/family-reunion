package com.familyreunion.rsvp.service

import com.familyreunion.rsvp.dto.VolunteerSignupDto
import com.familyreunion.rsvp.dto.VolunteerSignupRequest
import com.familyreunion.rsvp.dto.VolunteerTaskRequest
import com.familyreunion.rsvp.dto.VolunteerTaskResponse
import com.familyreunion.rsvp.exception.EventNotFoundException
import com.familyreunion.rsvp.exception.FamilyMemberNotFoundException
import com.familyreunion.rsvp.exception.VolunteerTaskNotFoundException
import com.familyreunion.rsvp.model.VolunteerSignup
import com.familyreunion.rsvp.model.VolunteerTask
import com.familyreunion.rsvp.repository.EventRepository
import com.familyreunion.rsvp.repository.FamilyMemberRepository
import com.familyreunion.rsvp.repository.VolunteerSignupRepository
import com.familyreunion.rsvp.repository.VolunteerTaskRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class VolunteerTaskService(
    private val volunteerTaskRepository: VolunteerTaskRepository,
    private val volunteerSignupRepository: VolunteerSignupRepository,
    private val eventRepository: EventRepository,
    private val familyMemberRepository: FamilyMemberRepository
) {

    companion object {
        const val TRIBUTE_TITLE_PREFIX = "Tribute to"
        const val MAX_TRIBUTE_SPEAKERS = 2
    }

    fun createTask(request: VolunteerTaskRequest): VolunteerTaskResponse {
        val event = eventRepository.findById(request.eventId)
            .orElseThrow { EventNotFoundException(request.eventId) }

        val task = VolunteerTask(
            title = request.title,
            description = request.description,
            event = event
        )
        return toResponse(volunteerTaskRepository.save(task))
    }

    @Transactional(readOnly = true)
    fun getAllTasks(): List<VolunteerTaskResponse> {
        return volunteerTaskRepository.findAll().map { toResponse(it) }
    }

    fun updateTask(id: Long, request: VolunteerTaskRequest): VolunteerTaskResponse {
        val existing = volunteerTaskRepository.findById(id)
            .orElseThrow { VolunteerTaskNotFoundException(id) }
        val event = eventRepository.findById(request.eventId)
            .orElseThrow { EventNotFoundException(request.eventId) }

        existing.title = request.title
        existing.description = request.description
        existing.event = event

        return toResponse(volunteerTaskRepository.save(existing))
    }

    fun deleteTask(id: Long) {
        if (!volunteerTaskRepository.existsById(id)) {
            throw VolunteerTaskNotFoundException(id)
        }
        volunteerTaskRepository.deleteById(id)
    }

    fun signUp(taskId: Long, request: VolunteerSignupRequest): VolunteerTaskResponse {
        val task = volunteerTaskRepository.findById(taskId)
            .orElseThrow { VolunteerTaskNotFoundException(taskId) }

        // Banquet tribute tasks are capped at two speakers; the frontend identifies
        // them the same way (title prefix), so keep the two checks in sync.
        if (task.title.trim().startsWith(TRIBUTE_TITLE_PREFIX)) {
            val existingIds = task.signups.mapNotNull { it.familyMember?.id }.toSet()
            val newIds = request.familyMemberIds.distinct().filter { it !in existingIds }
            if (existingIds.size + newIds.size > MAX_TRIBUTE_SPEAKERS) {
                throw IllegalStateException(
                    "This tribute already has the maximum of $MAX_TRIBUTE_SPEAKERS speakers"
                )
            }
        }

        for (memberId in request.familyMemberIds) {
            val member = familyMemberRepository.findById(memberId)
                .orElseThrow { FamilyMemberNotFoundException(memberId) }

            val existing = volunteerSignupRepository.findByTaskAndFamilyMember(task, member)
            if (existing == null) {
                val signup = VolunteerSignup(task = task, familyMember = member)
                volunteerSignupRepository.save(signup)
                task.signups.add(signup)
            }
        }

        return toResponse(task)
    }

    fun withdraw(taskId: Long, memberId: Long) {
        val task = volunteerTaskRepository.findById(taskId)
            .orElseThrow { VolunteerTaskNotFoundException(taskId) }
        val member = familyMemberRepository.findById(memberId)
            .orElseThrow { FamilyMemberNotFoundException(memberId) }

        volunteerSignupRepository.deleteByTaskAndFamilyMember(task, member)
    }

    private fun toResponse(task: VolunteerTask) = VolunteerTaskResponse(
        id = task.id,
        title = task.title,
        description = task.description,
        eventId = task.event?.id ?: 0,
        eventTitle = task.event?.title ?: "",
        eventDateTime = task.event?.eventDateTime ?: LocalDateTime.MIN,
        signups = task.signups.map {
            VolunteerSignupDto(
                id = it.id,
                familyMemberId = it.familyMember?.id ?: 0,
                familyMemberName = it.familyMember?.name ?: ""
            )
        },
        signupCount = task.signups.size
    )
}
