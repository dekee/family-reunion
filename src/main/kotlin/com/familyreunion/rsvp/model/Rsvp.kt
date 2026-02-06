package com.familyreunion.rsvp.model

import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "rsvps")
class Rsvp(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    var familyName: String = "",

    @Column(nullable = false)
    var headOfHouseholdName: String = "",

    @Column(nullable = false)
    var email: String = "",

    var phone: String? = null,

    @OneToMany(mappedBy = "rsvp", cascade = [CascadeType.ALL], orphanRemoval = true)
    val familyMembers: MutableList<FamilyMember> = mutableListOf(),

    @Column(nullable = false)
    var needsLodging: Boolean = false,

    var arrivalDate: LocalDate? = null,
    var departureDate: LocalDate? = null,

    var notes: String? = null
)
