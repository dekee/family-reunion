package com.familyreunion.rsvp.model

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "payment_line_items")
class PaymentLineItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    var payment: Payment? = null,

    var familyMemberId: Long? = null,

    var familyMemberName: String? = null,

    var guestName: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var ageGroup: AgeGroup = AgeGroup.ADULT,

    @Column(nullable = false, precision = 10, scale = 2)
    var amount: BigDecimal = BigDecimal.ZERO
)
