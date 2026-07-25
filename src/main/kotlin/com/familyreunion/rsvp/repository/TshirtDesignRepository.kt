package com.familyreunion.rsvp.repository

import com.familyreunion.rsvp.model.TshirtDesign
import org.springframework.data.jpa.repository.JpaRepository

interface TshirtDesignRepository : JpaRepository<TshirtDesign, Long> {
    fun findAllByOrderByIdAsc(): List<TshirtDesign>
    fun existsByName(name: String): Boolean
}
