package com.familyreunion.rsvp.model

import jakarta.persistence.*

@Entity
@Table(name = "family_members")
class FamilyMember(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    var name: String = "",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var ageGroup: AgeGroup = AgeGroup.ADULT,

    var dietaryNeeds: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rsvp_id", nullable = false)
    var rsvp: Rsvp? = null
)
