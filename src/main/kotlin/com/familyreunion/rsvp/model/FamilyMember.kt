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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    var parent: FamilyMember? = null,

    @OneToMany(mappedBy = "parent", cascade = [CascadeType.ALL])
    val children: MutableList<FamilyMember> = mutableListOf(),

    var generation: Int? = null,

    var isFounder: Boolean = false
)
