package com.familyreunion.rsvp.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "site_images")
class SiteImage(
    @Id
    @Column(name = "image_key")
    var key: String = "",

    @Column(nullable = false)
    var contentType: String = "",

    @Lob
    @Column(nullable = false)
    var data: ByteArray = ByteArray(0),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
