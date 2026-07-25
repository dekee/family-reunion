package com.familyreunion.rsvp.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "tshirt_design_votes",
    uniqueConstraints = [UniqueConstraint(columnNames = ["family_member_id"])]
)
class TshirtDesignVote(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "design_id", nullable = false)
    var design: TshirtDesign = TshirtDesign(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_member_id", nullable = false)
    var familyMember: FamilyMember = FamilyMember(),

    @Column(nullable = false)
    var votedAt: LocalDateTime = LocalDateTime.now()
)
