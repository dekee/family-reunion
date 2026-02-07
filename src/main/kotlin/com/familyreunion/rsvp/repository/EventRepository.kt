package com.familyreunion.rsvp.repository

import com.familyreunion.rsvp.model.Event
import org.springframework.data.jpa.repository.JpaRepository

interface EventRepository : JpaRepository<Event, Long> {
    fun findAllByOrderByEventDateTimeAsc(): List<Event>
}
