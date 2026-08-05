package com.familyreunion.rsvp.repository

import com.familyreunion.rsvp.model.VolunteerTask
import org.springframework.data.jpa.repository.JpaRepository

interface VolunteerTaskRepository : JpaRepository<VolunteerTask, Long>
