package com.familyreunion.rsvp.repository

import com.familyreunion.rsvp.model.TshirtSlogan
import org.springframework.data.jpa.repository.JpaRepository

interface TshirtSloganRepository : JpaRepository<TshirtSlogan, Long> {
    fun findAllByOrderByIdAsc(): List<TshirtSlogan>
    fun existsBySlogan(slogan: String): Boolean
}
