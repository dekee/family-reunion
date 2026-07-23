package com.familyreunion.rsvp.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "tributes",
    uniqueConstraints = [UniqueConstraint(columnNames = ["sibling_id", "author_id"])]
)
class Tribute(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sibling_id", nullable = false)
    var sibling: FamilyMember = FamilyMember(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    var author: FamilyMember = FamilyMember(),

    @Column(nullable = false, length = 4000)
    var story: String = "",

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
