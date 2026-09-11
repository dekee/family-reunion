package com.familyreunion.rsvp.model

enum class SizeCategory { UNISEX, YOUTH, ONESIE }

/**
 * T-shirt sizes offered to paid attendees. Enum names are the wire/DB format;
 * [label] is the human-readable text. Keep in sync with frontend/src/constants/tshirtSizes.ts.
 */
enum class TshirtSize(val label: String, val category: SizeCategory) {
    S("S", SizeCategory.UNISEX),
    M("M", SizeCategory.UNISEX),
    L("L", SizeCategory.UNISEX),
    XL("XL", SizeCategory.UNISEX),
    XXL("2XL", SizeCategory.UNISEX),
    XXXL("3XL", SizeCategory.UNISEX),
    XXXXL("4XL", SizeCategory.UNISEX),

    YS("YS", SizeCategory.YOUTH),
    YM("YM", SizeCategory.YOUTH),
    YL("YL", SizeCategory.YOUTH),
    YXL("YXL", SizeCategory.YOUTH),

    NEWBORN("Newborn", SizeCategory.ONESIE),
    M0_3("0-3 mths", SizeCategory.ONESIE),
    M3_6("3-6 mths", SizeCategory.ONESIE),
    M6_9("6-9 mths", SizeCategory.ONESIE),
    M9_12("9-12 mths", SizeCategory.ONESIE);

    fun isValidFor(ageGroup: AgeGroup): Boolean = category == categoryFor(ageGroup)

    companion object {
        fun categoryFor(ageGroup: AgeGroup): SizeCategory = when (ageGroup) {
            AgeGroup.ADULT, AgeGroup.SPOUSE -> SizeCategory.UNISEX
            AgeGroup.CHILD -> SizeCategory.YOUTH
            AgeGroup.INFANT -> SizeCategory.ONESIE
        }

        fun allowedFor(ageGroup: AgeGroup): List<TshirtSize> =
            entries.filter { it.category == categoryFor(ageGroup) }

        /**
         * Parses a size name submitted by a client and checks it is offered for the given age group.
         * Throws [IllegalArgumentException] (mapped to HTTP 400) when blank, unknown, or wrong category.
         */
        fun parseFor(raw: String?, ageGroup: AgeGroup, who: String): TshirtSize {
            val value = raw?.trim().orEmpty()
            if (value.isEmpty()) {
                throw IllegalArgumentException("T-shirt size is required for $who")
            }
            val size = entries.firstOrNull { it.name == value }
                ?: throw IllegalArgumentException("Unknown T-shirt size '$value' for $who")
            if (!size.isValidFor(ageGroup)) {
                val allowed = allowedFor(ageGroup).joinToString(", ") { it.label }
                throw IllegalArgumentException(
                    "Size ${size.label} is not available for $who (${ageGroup.name}). Choose from: $allowed"
                )
            }
            return size
        }
    }
}
