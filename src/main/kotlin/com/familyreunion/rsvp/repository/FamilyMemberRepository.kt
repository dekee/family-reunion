package com.familyreunion.rsvp.repository

import com.familyreunion.rsvp.model.FamilyMember
import org.springframework.data.jpa.repository.JpaRepository

interface FamilyMemberRepository : JpaRepository<FamilyMember, Long> {
    fun findByIsFounderTrue(): List<FamilyMember>
    fun findByParent(parent: FamilyMember): List<FamilyMember>
}
