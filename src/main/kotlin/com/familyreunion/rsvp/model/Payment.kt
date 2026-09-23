package com.familyreunion.rsvp.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "payments")
class Payment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /** Null for standalone Angel Fund gifts, which belong to no family branch. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rsvp_id")
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
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false, unique = true)
    var checkinToken: String = UUID.randomUUID().toString(),

    @Column(nullable = false)
    var checkedIn: Boolean = false,

    var checkedInAt: LocalDateTime? = null,

    var payerName: String? = null,

    var payerEmail: String? = null,

    /**
     * Donor-chosen display name for the public Angel Fund leaderboard. Deliberately not
     * [payerName]: the Stripe webhook overwrites that from the session's customer_details.
     */
    @Column(length = 80)
    var donorName: String? = null,

    @Column(length = 80)
    var donorFamilyLabel: String? = null,

    /** Anonymity is public-only — admins still see the Stripe cardholder name. */
    @Column(nullable = false)
    var donorAnonymous: Boolean = false,

    /** Admin moderation flag: hides the gift from the public leaderboard. */
    @Column(nullable = false)
    var donorHidden: Boolean = false,

    @OneToMany(mappedBy = "payment", cascade = [CascadeType.ALL], orphanRemoval = true)
    val lineItems: MutableList<PaymentLineItem> = mutableListOf()
)

enum class PaymentStatus {
    PENDING, COMPLETED, FAILED
}
