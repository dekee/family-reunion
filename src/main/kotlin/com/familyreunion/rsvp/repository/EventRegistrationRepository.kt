package com.familyreunion.rsvp.repository

import com.familyreunion.rsvp.model.Event
import com.familyreunion.rsvp.model.EventRegistration
import com.familyreunion.rsvp.model.FamilyMember
import org.springframework.data.jpa.repository.JpaRepository

interface EventRegistrationRepository : JpaRepository<EventRegistration, Long> {
    fun findByEventAndFamilyMember(event: Event, familyMember: FamilyMember): EventRegistration?
    fun deleteByEventAndFamilyMember(event: Event, familyMember: FamilyMember)
}
