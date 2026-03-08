package com.familyreunion.rsvp.service

import com.familyreunion.rsvp.dto.*
import com.familyreunion.rsvp.exception.EventNotFoundException
import com.familyreunion.rsvp.exception.FamilyMemberNotFoundException
import com.familyreunion.rsvp.model.Event
import com.familyreunion.rsvp.model.EventRegistration
import com.familyreunion.rsvp.repository.EventRegistrationRepository
import com.familyreunion.rsvp.repository.EventRepository
import com.familyreunion.rsvp.repository.FamilyMemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class EventService(
    private val eventRepository: EventRepository,
    private val eventRegistrationRepository: EventRegistrationRepository,
    private val familyMemberRepository: FamilyMemberRepository
) {

    fun createEvent(request: EventRequest): EventResponse {
        val event = Event(
            title = request.title,
            description = request.description,
            eventDateTime = request.eventDateTime,
            address = request.address,
            hostName = request.hostName,
            notes = request.notes
        )
        val saved = eventRepository.save(event)
        return toResponse(saved)
    }

    @Transactional(readOnly = true)
    fun getAllEvents(): List<EventResponse> {
        return eventRepository.findAllByOrderByEventDateTimeAsc().map { toResponse(it) }
    }

    fun updateEvent(id: Long, request: EventRequest): EventResponse {
        val existing = eventRepository.findById(id)
            .orElseThrow { EventNotFoundException(id) }

        existing.title = request.title
        existing.description = request.description
        existing.eventDateTime = request.eventDateTime
        existing.address = request.address
        existing.hostName = request.hostName
        existing.notes = request.notes

        val saved = eventRepository.save(existing)
        return toResponse(saved)
    }

    fun deleteEvent(id: Long) {
        if (!eventRepository.existsById(id)) {
            throw EventNotFoundException(id)
        }
        eventRepository.deleteById(id)
    }

    fun registerMembers(eventId: Long, request: EventRegisterRequest): EventResponse {
        val event = eventRepository.findById(eventId)
            .orElseThrow { EventNotFoundException(eventId) }

        for (memberId in request.familyMemberIds) {
            val member = familyMemberRepository.findById(memberId)
                .orElseThrow { FamilyMemberNotFoundException(memberId) }

            val existing = eventRegistrationRepository.findByEventAndFamilyMember(event, member)
            if (existing == null) {
                val registration = EventRegistration(event = event, familyMember = member)
                eventRegistrationRepository.save(registration)
                event.registrations.add(registration)
            }
        }

        return toResponse(event)
    }

    fun unregisterMember(eventId: Long, memberId: Long) {
        val event = eventRepository.findById(eventId)
            .orElseThrow { EventNotFoundException(eventId) }
        val member = familyMemberRepository.findById(memberId)
            .orElseThrow { FamilyMemberNotFoundException(memberId) }

        eventRegistrationRepository.deleteByEventAndFamilyMember(event, member)
    }

    private fun toResponse(event: Event) = EventResponse(
        id = event.id,
        title = event.title,
        description = event.description,
        eventDateTime = event.eventDateTime,
        address = event.address,
        hostName = event.hostName,
        notes = event.notes,
        registrations = event.registrations.map {
            EventRegistrationDto(
                id = it.id,
                familyMemberId = it.familyMember?.id ?: 0,
                familyMemberName = it.familyMember?.name ?: ""
            )
        },
        registrationCount = event.registrations.size
    )
}
