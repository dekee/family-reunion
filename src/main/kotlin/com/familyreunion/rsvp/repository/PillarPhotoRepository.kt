package com.familyreunion.rsvp.repository

import com.familyreunion.rsvp.model.PillarPhoto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface PillarPhotoVersion {
    val siblingId: Long
    val updatedAt: LocalDateTime
}

interface PillarPhotoRepository : JpaRepository<PillarPhoto, Long> {
    // Metadata only — the list endpoint must not load every image's bytes.
    @Query("SELECT p.siblingId AS siblingId, p.updatedAt AS updatedAt FROM PillarPhoto p")
    fun findAllVersions(): List<PillarPhotoVersion>
}
