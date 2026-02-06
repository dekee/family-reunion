package com.familyreunion.rsvp.dto

import com.familyreunion.rsvp.model.AgeGroup

data class FamilyTreeNode(
    val id: Long,
    val name: String,
    val generation: Int?,
    val ageGroup: AgeGroup,
    val parentId: Long? = null,
    val children: List<FamilyTreeNode> = emptyList()
)

data class FamilyTreeResponse(
    val roots: List<FamilyTreeNode>,
    val totalMembers: Int
)
