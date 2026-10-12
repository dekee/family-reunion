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

    /** The full-size original, exactly as uploaded. */
    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(nullable = false, columnDefinition = "bytea")
    var data: ByteArray = ByteArray(0),

    /** Small JPEG for the page's portrait circles; null for photos uploaded before originals were kept. */
    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(columnDefinition = "bytea")
    var thumbnail: ByteArray? = null,

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
