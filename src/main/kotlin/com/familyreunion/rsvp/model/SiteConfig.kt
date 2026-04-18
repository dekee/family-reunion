package com.familyreunion.rsvp.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "site_config")
class SiteConfig(
    @Id
    @Column(name = "config_key")
    var key: String = "",

    @Column(name = "config_value", nullable = false, columnDefinition = "TEXT")
    var value: String = "",

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
