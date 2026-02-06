package com.familyreunion.rsvp.dto

import com.familyreunion.rsvp.model.AgeGroup
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class FamilyMemberDto(
    val id: Long? = null,
    @field:NotBlank val name: String = "",
    @field:NotNull val ageGroup: AgeGroup = AgeGroup.ADULT,
    val dietaryNeeds: String? = null
)
