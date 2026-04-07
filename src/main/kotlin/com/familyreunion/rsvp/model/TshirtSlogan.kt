package com.familyreunion.rsvp.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "tshirt_slogans")
class TshirtSlogan(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true, length = 500)
    var slogan: String = "",

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @OneToMany(mappedBy = "slogan", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val votes: MutableList<TshirtVote> = mutableListOf()
)
