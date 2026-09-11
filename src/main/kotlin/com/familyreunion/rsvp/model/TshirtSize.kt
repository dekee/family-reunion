package com.familyreunion.rsvp.model

enum class SizeCategory { UNISEX, YOUTH, ONESIE }

/**
 * T-shirt sizes offered to paid attendees. Enum names are the wire/DB format;
 * [label] is the human-readable text. Keep in sync with frontend/src/constants/tshirtSizes.ts.
 *
 * Any attendee may pick any size — age group only decides which group is suggested first
 * in the UI (a big kid may need an adult shirt, a small adult a youth one).
 */
enum class TshirtSize(val label: String, val category: SizeCategory) {
    S("S", SizeCategory.UNISEX),
    M("M", SizeCategory.UNISEX),
    L("L", SizeCategory.UNISEX),
    XL("XL", SizeCategory.UNISEX),
    XXL("2XL", SizeCategory.UNISEX),
    XXXL("3XL", SizeCategory.UNISEX),
    XXXXL("4XL", SizeCategory.UNISEX),

    YS("Youth S", SizeCategory.YOUTH),
    YM("Youth M", SizeCategory.YOUTH),
    YL("Youth L", SizeCategory.YOUTH),
    YXL("Youth XL", SizeCategory.YOUTH),

    NEWBORN("Newborn", SizeCategory.ONESIE),
    M0_3("0-3 mths", SizeCategory.ONESIE),
    M3_6("3-6 mths", SizeCategory.ONESIE),
    M6_9("6-9 mths", SizeCategory.ONESIE),
    M9_12("9-12 mths", SizeCategory.ONESIE);

    companion object {
        /** The size group most likely to fit an age group; used for UI ordering only, not validation. */
        fun suggestedCategoryFor(ageGroup: AgeGroup): SizeCategory = when (ageGroup) {
            AgeGroup.ADULT, AgeGroup.SPOUSE -> SizeCategory.UNISEX
            AgeGroup.CHILD -> SizeCategory.YOUTH
            AgeGroup.INFANT -> SizeCategory.ONESIE
        }

        /**
         * Parses a size name submitted by a client.
         * Throws [IllegalArgumentException] (mapped to HTTP 400) when blank or unknown.
         */
        fun parse(raw: String?, who: String): TshirtSize {
            val value = raw?.trim().orEmpty()
            if (value.isEmpty()) {
                throw IllegalArgumentException("T-shirt size is required for $who")
            }
            return entries.firstOrNull { it.name == value }
                ?: throw IllegalArgumentException("Unknown T-shirt size '$value' for $who")
        }
    }
}
