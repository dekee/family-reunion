package com.familyreunion.rsvp.model

import jakarta.persistence.*

@Entity
@Table(name = "attendees")
class Attendee(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rsvp_id", nullable = false)
    var rsvp: Rsvp? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_member_id")
    var familyMember: FamilyMember? = null,

    var guestName: String? = null,

    @Enumerated(EnumType.STRING)
    var guestAgeGroup: AgeGroup? = null,

    var dietaryNeeds: String? = null
) {
    val name: String
        get() = familyMember?.name ?: guestName ?: ""

    val ageGroup: AgeGroup
        get() = familyMember?.ageGroup ?: guestAgeGroup ?: AgeGroup.ADULT

    val isGuest: Boolean
        get() = familyMember == null
}
