package com.familyreunion.rsvp.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "payments")
class Payment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rsvp_id", nullable = false)
    var rsvp: Rsvp? = null,

    @Column(nullable = false, precision = 10, scale = 2)
    var amount: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false, unique = true)
    var stripeSessionId: String = "",

    var stripePaymentIntentId: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: PaymentStatus = PaymentStatus.PENDING,

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
)

enum class PaymentStatus {
    PENDING, COMPLETED, FAILED
}
