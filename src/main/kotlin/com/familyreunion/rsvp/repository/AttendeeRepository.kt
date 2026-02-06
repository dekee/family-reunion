package com.familyreunion.rsvp.repository

import com.familyreunion.rsvp.model.Attendee
import org.springframework.data.jpa.repository.JpaRepository

interface AttendeeRepository : JpaRepository<Attendee, Long>
