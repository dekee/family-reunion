package com.familyreunion.rsvp.repository

import com.familyreunion.rsvp.model.FamilyMember
import com.familyreunion.rsvp.model.TshirtDesignVote
import org.springframework.data.jpa.repository.JpaRepository

interface TshirtDesignVoteRepository : JpaRepository<TshirtDesignVote, Long> {
    fun findByFamilyMember(familyMember: FamilyMember): TshirtDesignVote?
}
