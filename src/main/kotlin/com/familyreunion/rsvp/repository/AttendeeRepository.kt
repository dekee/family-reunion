package com.familyreunion.rsvp.repository

import com.familyreunion.rsvp.model.Attendee
import com.familyreunion.rsvp.model.FamilyMember
import org.springframework.data.jpa.repository.JpaRepository

interface AttendeeRepository : JpaRepository<Attendee, Long> {
    fun findByFamilyMemberIn(members: Collection<FamilyMember>): List<Attendee>
}
