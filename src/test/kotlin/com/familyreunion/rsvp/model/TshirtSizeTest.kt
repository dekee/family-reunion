package com.familyreunion.rsvp.model

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class TshirtSizeTest {

    @Test
    fun `adults and spouses get the seven unisex sizes`() {
        val expected = listOf("S", "M", "L", "XL", "2XL", "3XL", "4XL")
        assertThat(TshirtSize.allowedFor(AgeGroup.ADULT).map { it.label }).containsExactlyElementsOf(expected)
        assertThat(TshirtSize.allowedFor(AgeGroup.SPOUSE)).isEqualTo(TshirtSize.allowedFor(AgeGroup.ADULT))
    }

    @Test
    fun `children get youth sizes`() {
        assertThat(TshirtSize.allowedFor(AgeGroup.CHILD).map { it.label })
            .containsExactly("YS", "YM", "YL", "YXL")
    }

    @Test
    fun `infants get onesie sizes only`() {
        assertThat(TshirtSize.allowedFor(AgeGroup.INFANT).map { it.label })
            .containsExactly("Newborn", "0-3 mths", "3-6 mths", "6-9 mths", "9-12 mths")
    }

    @Test
    fun `every size belongs to exactly one age group category`() {
        val all = AgeGroup.entries.flatMap { TshirtSize.allowedFor(it) }.toSet()
        assertThat(all).containsExactlyInAnyOrderElementsOf(TshirtSize.entries)
    }

    @Test
    fun `parseFor accepts a valid size for the age group`() {
        assertThat(TshirtSize.parseFor("M0_3", AgeGroup.INFANT, "Baby")).isEqualTo(TshirtSize.M0_3)
        assertThat(TshirtSize.parseFor(" XXL ", AgeGroup.SPOUSE, "Pat")).isEqualTo(TshirtSize.XXL)
    }

    @Test
    fun `parseFor rejects blank size`() {
        assertThatThrownBy { TshirtSize.parseFor("", AgeGroup.CHILD, "Kid") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("required for Kid")
        assertThatThrownBy { TshirtSize.parseFor(null, AgeGroup.CHILD, "Kid") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `parseFor rejects unknown size`() {
        assertThatThrownBy { TshirtSize.parseFor("HUGE", AgeGroup.ADULT, "Pat") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Unknown T-shirt size 'HUGE'")
    }

    @Test
    fun `parseFor rejects a size from the wrong category`() {
        assertThatThrownBy { TshirtSize.parseFor("YS", AgeGroup.ADULT, "Pat") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("not available for Pat")
        assertThatThrownBy { TshirtSize.parseFor("NEWBORN", AgeGroup.CHILD, "Kid") }
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { TshirtSize.parseFor("L", AgeGroup.INFANT, "Baby") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }
}
