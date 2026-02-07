package com.familyreunion.rsvp.model

import jakarta.persistence.*

@Entity
@Table(
    name = "event_registrations",
    uniqueConstraints = [UniqueConstraint(columnNames = ["event_id", "family_member_id"])]
)
class EventRegistration(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    var event: Event? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_member_id", nullable = false)
    var familyMember: FamilyMember? = null
)
