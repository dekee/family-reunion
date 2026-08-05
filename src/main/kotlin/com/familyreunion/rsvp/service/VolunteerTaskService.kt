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
