package com.familyreunion.rsvp.service

import com.familyreunion.rsvp.dto.MeetingRequest
import com.familyreunion.rsvp.dto.MeetingResponse
import com.familyreunion.rsvp.exception.MeetingNotFoundException
import com.familyreunion.rsvp.model.Meeting
import com.familyreunion.rsvp.repository.MeetingRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class MeetingService(private val meetingRepository: MeetingRepository) {

    fun createMeeting(request: MeetingRequest): MeetingResponse {
        val meeting = Meeting(
            title = request.title,
            meetingDateTime = request.meetingDateTime,
            zoomLink = request.zoomLink,
            phoneNumber = request.phoneNumber,
            meetingId = request.meetingId,
            passcode = request.passcode,
            notes = request.notes
        )
        val saved = meetingRepository.save(meeting)
        return toResponse(saved)
    }

    @Transactional(readOnly = true)
    fun getAllMeetings(): List<MeetingResponse> {
        return meetingRepository.findAllByOrderByMeetingDateTimeAsc().map { toResponse(it) }
    }

    @Transactional(readOnly = true)
    fun getMeetingById(id: Long): MeetingResponse {
        val meeting = meetingRepository.findById(id)
            .orElseThrow { MeetingNotFoundException(id) }
        return toResponse(meeting)
    }

    fun updateMeeting(id: Long, request: MeetingRequest): MeetingResponse {
        val existing = meetingRepository.findById(id)
            .orElseThrow { MeetingNotFoundException(id) }

        existing.title = request.title
        existing.meetingDateTime = request.meetingDateTime
        existing.zoomLink = request.zoomLink
        existing.phoneNumber = request.phoneNumber
        existing.meetingId = request.meetingId
        existing.passcode = request.passcode
        existing.notes = request.notes

        val saved = meetingRepository.save(existing)
        return toResponse(saved)
    }

    fun deleteMeeting(id: Long) {
        if (!meetingRepository.existsById(id)) {
            throw MeetingNotFoundException(id)
        }
        meetingRepository.deleteById(id)
    }

    private fun toResponse(meeting: Meeting) = MeetingResponse(
        id = meeting.id,
        title = meeting.title,
        meetingDateTime = meeting.meetingDateTime,
        zoomLink = meeting.zoomLink,
        phoneNumber = meeting.phoneNumber,
        meetingId = meeting.meetingId,
        passcode = meeting.passcode,
        notes = meeting.notes
    )
}
