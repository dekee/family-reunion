package com.familyreunion.rsvp.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "tshirt_votes",
    uniqueConstraints = [UniqueConstraint(columnNames = ["voter_name"])]
)
class TshirtVote(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slogan_id", nullable = false)
    var slogan: TshirtSlogan = TshirtSlogan(),

    @Column(name = "voter_name", nullable = false)
    var voterName: String = "",

    @Column(nullable = false)
    var votedAt: LocalDateTime = LocalDateTime.now()
)
