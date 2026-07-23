package com.familyreunion.rsvp.repository

import com.familyreunion.rsvp.model.FamilyMember
import com.familyreunion.rsvp.model.Tribute
import org.springframework.data.jpa.repository.JpaRepository

interface TributeRepository : JpaRepository<Tribute, Long> {
    fun findAllByOrderByCreatedAtDesc(): List<Tribute>
    fun findBySiblingAndAuthor(sibling: FamilyMember, author: FamilyMember): Tribute?
}
