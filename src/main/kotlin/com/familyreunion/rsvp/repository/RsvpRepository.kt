package com.familyreunion.rsvp.repository

import com.familyreunion.rsvp.model.Rsvp
import org.springframework.data.jpa.repository.JpaRepository

interface RsvpRepository : JpaRepository<Rsvp, Long>
