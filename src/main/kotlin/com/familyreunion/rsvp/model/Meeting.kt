package com.familyreunion.rsvp.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "meetings")
class Meeting(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    var title: String = "",

    @Column(nullable = false)
    var meetingDateTime: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var zoomLink: String = "",

    var phoneNumber: String? = null,

    var meetingId: String? = null,

    var passcode: String? = null,

    var notes: String? = null
)
