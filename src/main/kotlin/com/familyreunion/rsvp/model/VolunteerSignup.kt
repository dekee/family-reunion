package com.familyreunion.rsvp.model

import jakarta.persistence.*

@Entity
@Table(
    name = "volunteer_signups",
    uniqueConstraints = [UniqueConstraint(columnNames = ["volunteer_task_id", "family_member_id"])]
)
class VolunteerSignup(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "volunteer_task_id", nullable = false)
    var task: VolunteerTask? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_member_id", nullable = false)
    var familyMember: FamilyMember? = null
)
