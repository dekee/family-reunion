package com.familyreunion.rsvp.dto

import com.familyreunion.rsvp.model.AgeGroup
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class FamilyMemberRequest(
    @field:NotBlank(message = "Name is required")
    val name: String,

    @field:NotNull(message = "Age group is required")
    val ageGroup: AgeGroup,

    val parentId: Long? = null,

    val generation: Int? = null
)

data class MoveMemberRequest(
    val newParentId: Long? = null
)
