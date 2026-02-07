package com.familyreunion.rsvp.repository

import com.familyreunion.rsvp.model.Meeting
import org.springframework.data.jpa.repository.JpaRepository

interface MeetingRepository : JpaRepository<Meeting, Long> {
    fun findAllByOrderByMeetingDateTimeAsc(): List<Meeting>
}
