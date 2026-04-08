package com.familyreunion.rsvp.repository

import com.familyreunion.rsvp.model.FamilyMember
import com.familyreunion.rsvp.model.TshirtVote
import org.springframework.data.jpa.repository.JpaRepository

interface TshirtVoteRepository : JpaRepository<TshirtVote, Long> {
    fun findByFamilyMember(familyMember: FamilyMember): TshirtVote?
}
