package com.familyreunion.rsvp.repository

import com.familyreunion.rsvp.model.FamilyMember
import com.familyreunion.rsvp.model.VolunteerSignup
import com.familyreunion.rsvp.model.VolunteerTask
import org.springframework.data.jpa.repository.JpaRepository

interface VolunteerSignupRepository : JpaRepository<VolunteerSignup, Long> {
    fun findByTaskAndFamilyMember(task: VolunteerTask, familyMember: FamilyMember): VolunteerSignup?
    fun deleteByTaskAndFamilyMember(task: VolunteerTask, familyMember: FamilyMember)
}
