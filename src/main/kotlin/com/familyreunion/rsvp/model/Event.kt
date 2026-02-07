package com.familyreunion.rsvp.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "events")
class Event(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    var title: String = "",

    var description: String? = null,

    @Column(nullable = false)
    var eventDateTime: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var address: String = "",

    var hostName: String? = null,

    var notes: String? = null,

    @OneToMany(mappedBy = "event", cascade = [CascadeType.ALL], orphanRemoval = true)
    val registrations: MutableList<EventRegistration> = mutableListOf()
)
