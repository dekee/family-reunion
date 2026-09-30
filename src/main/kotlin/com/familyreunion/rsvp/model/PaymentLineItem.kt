package com.familyreunion.rsvp.model

import jakarta.persistence.*
import java.math.BigDecimal

const val ANGEL_CONTRIBUTION_NAME = "Angel Contribution"
const val DONATION_LINE_ITEM_NAME = "Donation"

/**
 * Refusal both size-edit endpoints give for a row that never bought a shirt. Lives beside the
 * [PaymentLineItem.hasShirt] rule it explains, so the two cannot drift apart.
 */
const val NO_SHIRT_MESSAGE = "This attendee does not have a T-shirt — add one from the donations page."

/**
 * What a line item represents. Before this existed, "is this money rather than a person?" was the
 * string sentinel [ANGEL_CONTRIBUTION_NAME] in `guestName`. That only worked while a payment held
 * exactly one kind of non-person row; donation checkouts put a donation row and attendee rows on the
 * same payment, and need shirt revenue told apart from fee revenue.
 *
 * - [FEE]      a person who paid their full age-group fee (every pre-existing non-angel row)
 * - [SHIRT]    a person covered by a donation who also bought a T-shirt
 * - [ATTENDEE] a person covered by a donation with no shirt — a $0 row, the record that they are coming
 * - [DONATION] the freeform portion of a donation checkout; counts toward the branch balance
 * - [ANGEL]    an Angel Fund gift; deliberately excluded from branch balances
 */
enum class LineItemKind { FEE, SHIRT, ATTENDEE, DONATION, ANGEL }

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
    var amount: BigDecimal = BigDecimal.ZERO,

    /** Null for line items created before sizes were collected, and always null unless [hasShirt]. */
    @Enumerated(EnumType.STRING)
    @Column(name = "tshirt_size", length = 20)
    var tshirtSize: TshirtSize? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 20)
    var kind: LineItemKind = LineItemKind.FEE
) {
    /**
     * The `guestName` sentinel is kept as a backstop alongside [kind]: the V9 backfill keys off that
     * same string, so a row it somehow missed still reads as angel money rather than silently
     * counting toward a branch balance.
     */
    val isAngel: Boolean
        get() = kind == LineItemKind.ANGEL || guestName == ANGEL_CONTRIBUTION_NAME

    /** A person to admit, as opposed to money with no body attached. */
    val isPerson: Boolean
        get() = !isAngel && kind != LineItemKind.DONATION

    /**
     * Entitles someone to a shirt, and therefore to a size. [isAngel] is checked first for the same
     * reason [isPerson] does: a pre-V9 angel row defaults to [LineItemKind.FEE] and is recognised
     * only by its name sentinel, and must not read as a shirt.
     */
    val hasShirt: Boolean
        get() = !isAngel && (kind == LineItemKind.FEE || kind == LineItemKind.SHIRT)

    val displayName: String
        get() = familyMemberName ?: guestName ?: "Unknown"
}
