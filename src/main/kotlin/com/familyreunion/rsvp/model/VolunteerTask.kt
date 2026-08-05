package com.familyreunion.rsvp.model

import jakarta.persistence.*

@Entity
@Table(name = "volunteer_tasks")
class VolunteerTask(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    var title: String = "",

    var description: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    var event: Event? = null,

    @OneToMany(mappedBy = "task", cascade = [CascadeType.ALL], orphanRemoval = true)
    val signups: MutableList<VolunteerSignup> = mutableListOf()
)
