package com.familyreunion.rsvp.model

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class TshirtSizeTest {

    @Test
    fun `offers seven unisex, four youth and five onesie sizes`() {
        val byCategory = TshirtSize.entries.groupBy { it.category }
        assertThat(byCategory[SizeCategory.UNISEX]!!.map { it.label })
            .containsExactly("S", "M", "L", "XL", "2XL", "3XL", "4XL")
        assertThat(byCategory[SizeCategory.YOUTH]!!.map { it.label })
            .containsExactly("Youth S", "Youth M", "Youth L", "Youth XL")
        assertThat(byCategory[SizeCategory.ONESIE]!!.map { it.label })
            .containsExactly("Newborn", "0-3 mths", "3-6 mths", "6-9 mths", "9-12 mths")
    }

    @Test
    fun `suggested category follows age group but spouse matches adult`() {
        assertThat(TshirtSize.suggestedCategoryFor(AgeGroup.ADULT)).isEqualTo(SizeCategory.UNISEX)
        assertThat(TshirtSize.suggestedCategoryFor(AgeGroup.SPOUSE)).isEqualTo(SizeCategory.UNISEX)
        assertThat(TshirtSize.suggestedCategoryFor(AgeGroup.CHILD)).isEqualTo(SizeCategory.YOUTH)
        assertThat(TshirtSize.suggestedCategoryFor(AgeGroup.INFANT)).isEqualTo(SizeCategory.ONESIE)
    }

    @Test
    fun `parse accepts any known size regardless of age group`() {
        assertThat(TshirtSize.parse("M0_3", "Baby")).isEqualTo(TshirtSize.M0_3)
        assertThat(TshirtSize.parse(" XXL ", "Pat")).isEqualTo(TshirtSize.XXL)
        // Big kid in an adult shirt, small adult in a youth shirt — both fine
        assertThat(TshirtSize.parse("L", "Big Kid")).isEqualTo(TshirtSize.L)
        assertThat(TshirtSize.parse("YXL", "Small Adult")).isEqualTo(TshirtSize.YXL)
    }

    @Test
    fun `parse rejects blank size`() {
        assertThatThrownBy { TshirtSize.parse("", "Kid") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("required for Kid")
        assertThatThrownBy { TshirtSize.parse(null, "Kid") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `parse rejects unknown size`() {
        assertThatThrownBy { TshirtSize.parse("HUGE", "Pat") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Unknown T-shirt size 'HUGE'")
    }
}
