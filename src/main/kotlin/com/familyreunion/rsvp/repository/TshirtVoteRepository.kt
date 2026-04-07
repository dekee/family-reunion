package com.familyreunion.rsvp.repository

import com.familyreunion.rsvp.model.TshirtSlogan
import com.familyreunion.rsvp.model.TshirtVote
import org.springframework.data.jpa.repository.JpaRepository

interface TshirtVoteRepository : JpaRepository<TshirtVote, Long> {
    fun findByVoterName(voterName: String): TshirtVote?
    fun countBySlogan(slogan: TshirtSlogan): Long
}
