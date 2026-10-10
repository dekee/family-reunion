package com.familyreunion.rsvp.model

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

/** Portrait of one of the 11 pillars, keyed by the pillar's family member id. */
@Entity
@Table(name = "pillar_photos")
class PillarPhoto(
    @Id
    @Column(name = "sibling_id")
    val siblingId: Long = 0,

    @Column(nullable = false, length = 100)
    var contentType: String = "image/jpeg",

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(nullable = false, columnDefinition = "bytea")
    var data: ByteArray = ByteArray(0),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
