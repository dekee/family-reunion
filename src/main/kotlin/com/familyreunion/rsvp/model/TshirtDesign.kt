package com.familyreunion.rsvp.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "tshirt_designs")
class TshirtDesign(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true, length = 200)
    var name: String = "",

    @Column(nullable = false, length = 500)
    var imageUrl: String = "",

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @OneToMany(mappedBy = "design", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val votes: MutableList<TshirtDesignVote> = mutableListOf()
)
